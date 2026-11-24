package com.example.demo.service.ai;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.example.demo.Entity.AiTurn;
import com.example.demo.enums.AiTurnStatus;
import com.example.demo.service.tool.AgentPrincipal;
import com.example.demo.service.tool.GameToolDispatcher;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class AiAgentService {

    private static final int MAX_ROUNDS = 4;
    private static final int MAX_TOOL_CALLS = 8;

    private final DeepSeekClient deepSeekClient;
    private final GameToolDispatcher gameToolDispatcher;
    private final ObjectMapper mapper;
    private final AiTurnPersistenceService turnPersistence;

    public AiAgentService(
            DeepSeekClient deepSeekClient,
            GameToolDispatcher gameToolDispatcher,
            ObjectMapper mapper,
            AiTurnPersistenceService turnPersistence) {
        this.deepSeekClient = deepSeekClient;
        this.gameToolDispatcher = gameToolDispatcher;
        this.mapper = mapper;
        this.turnPersistence = turnPersistence;
    }

    /** Ordinary stateless chat: no Agent, no AiTurn and only stateless tools. */
    public String chat(String prompt) {
        return chatInternal(null, prompt);
    }

    /** Agent chat: the turn is persisted before model/tool execution. */
    public String chatAsAgent(AgentPrincipal principal, String prompt) {
        if (principal == null) {
            throw new IllegalArgumentException("Agent principal is required");
        }
        gameToolDispatcher.requireAgentLaunch(principal);
        return chatInternal(principal, prompt);
    }

    private String chatInternal(AgentPrincipal principal, String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Prompt cannot be empty");
        }

        List<Object> messages = new ArrayList<>();
        messages.add(Map.of(
                "role", "system",
                "content", "You are a TRPG assistant. Use tools when necessary. "
                        + "Never invent dice results or action outcomes. "
                        + "Review dependencies before proposing tool calls. "
                        + "Prefer one state-changing tool call per round and inspect its result "
                        + "before deciding on another. Read-only calls may be grouped."));
        messages.add(Map.of("role", "user", "content", prompt));

        // chat() has no persistent turn, but should still be able to answer and roll dice.
        if (principal == null) {
            return continueChat(null, null, null, messages, 0, 0);
        }

        Long turnId = turnPersistence.createTurn(principal, messages);
        if (turnId == null) {
            throw new IllegalStateException("AI turn was not persisted");
        }
        String leaseToken = turnPersistence.acquireLease(principal, turnId);
        if (leaseToken == null || leaseToken.isBlank()) {
            throw new IllegalStateException("AI turn lease was not acquired");
        }
        try {
            return continueChat(principal, turnId, leaseToken, messages, 0, 0);
        } finally {
            turnPersistence.releaseLease(principal, turnId, leaseToken);
        }
    }

    /** Resume an existing turn; do not create a new conversation or call ID. */
    public String resumeTurn(AgentPrincipal principal, Long turnId) {
        if (principal == null) {
            throw new IllegalArgumentException("Agent principal is required");
        }
        gameToolDispatcher.requireAgentLaunch(principal);

        AiTurn existing = turnPersistence.loadTurn(principal, turnId);
        if (existing.getStatus() == AiTurnStatus.COMPLETED) {
            return existing.getFinalResponse();
        }

        String leaseToken = turnPersistence.acquireLease(principal, turnId);
        if (leaseToken == null || leaseToken.isBlank()) {
            throw new IllegalStateException("AI turn lease was not acquired");
        }
        try {
            AiTurn turn = turnPersistence.loadTurn(principal, turnId);
            if (turn.getStatus() == AiTurnStatus.WAITING_TOOL) {
                resumePendingTools(principal, turnId, leaseToken);
                turn = turnPersistence.loadTurn(principal, turnId);
            }

            if (turn.getStatus() != AiTurnStatus.RUNNING) {
                throw new IllegalStateException(
                        "AI turn cannot continue: " + turn.getStatus());
            }

            List<Object> messages = new ArrayList<>(mapper.readValue(
                    turn.getMessagesJson(),
                    new TypeReference<List<Object>>() {}));

            return continueChat(
                    principal,
                    turnId,
                    leaseToken,
                    messages,
                    turn.getRoundCount(),
                    turn.getToolCallCount());
        } finally {
            turnPersistence.releaseLease(principal, turnId, leaseToken);
        }
    }

    /** Finish a pending batch before making another model request. */
    private void resumePendingTools(
            AgentPrincipal principal, Long turnId, String leaseToken) {

        renewLeaseIfNeeded(principal, turnId, leaseToken);
        AiTurn turn = turnPersistence.loadTurn(principal, turnId);
        if (turn.getStatus() != AiTurnStatus.WAITING_TOOL) {
            throw new IllegalStateException("Turn is not waiting for tools");
        }

        JsonNode messages = mapper.readTree(turn.getMessagesJson());
        if (!messages.isArray()) {
            throw new IllegalStateException("Invalid saved messages");
        }

        int assistantIndex = -1;
        JsonNode calls = null;
        for (int i = messages.size() - 1; i >= 0; i--) {
            JsonNode msg = messages.get(i);
            if ("assistant".equals(msg.path("role").asText())
                    && msg.path("tool_calls").isArray()) {
                assistantIndex = i;
                calls = msg.path("tool_calls");
                break;
            }
        }
        if (assistantIndex < 0 || calls == null || calls.isEmpty()) {
            throw new IllegalStateException("Pending tool calls not found");
        }

        Set<String> completedIds = new HashSet<>();
        for (int i = assistantIndex + 1; i < messages.size(); i++) {
            JsonNode msg = messages.get(i);
            if ("tool".equals(msg.path("role").asText())) {
                completedIds.add(msg.path("tool_call_id").asText());
            }
        }

        for (JsonNode call : calls) {
            String callId = call.path("id").asText();
            if (completedIds.contains(callId)) {
                continue;
            }
            validateToolCall(principal, call);
            String toolName = call.path("function").path("name").asText();
            JsonNode arguments = mapper.readTree(
                    call.path("function").path("arguments").asText());

            renewLeaseIfNeeded(principal, turnId, leaseToken);
            // execute_action is deduplicated by its original call ID.
            Object result = gameToolDispatcher.dispatchAgent(
                    principal, toolName, arguments, callId);
            renewLeaseIfNeeded(principal, turnId, leaseToken);
            turnPersistence.saveToolResult(principal, turnId, callId, result);
            completedIds.add(callId);
        }
    }

    private String continueChat(
            AgentPrincipal principal,
            Long turnId,
            String leaseToken,
            List<Object> messages,
            int startRound,
            int totalToolCalls) {

        List<Object> tools = new ArrayList<>(diceToolDefinition());
        if (principal != null) {
            tools.add(actionToolDefinition());
            tools.add(characterToolDefinition());
            tools.add(availableActionsToolDefinition());
        }

        for (int round = startRound; round < MAX_ROUNDS; round++) {
            renewLeaseIfNeeded(principal, turnId, leaseToken);
            JsonNode response = deepSeekClient.chatWithTools(
                    List.copyOf(messages), tools, "auto");
            // A long HTTP request may have outlived our lease.
            renewLeaseIfNeeded(principal, turnId, leaseToken);

            JsonNode choice = response.path("choices").get(0);
            if (choice == null) {
                throw new IllegalStateException("AI response has no choice");
            }
            String finishReason = choice.path("finish_reason").asText();
            JsonNode message = choice.path("message");

            if ("stop".equals(finishReason)) {
                JsonNode content = message.path("content");
                if (!content.isTextual() || content.asText().isBlank()) {
                    throw new IllegalStateException("AI returned no text");
                }
                if (turnId != null) {
                    turnPersistence.completeTurn(
                            principal, turnId, content.asText());
                }
                return content.asText();
            }

            if (!"tool_calls".equals(finishReason)) {
                throw new IllegalStateException(
                        "Unexpected finish reason: " + finishReason);
            }
            JsonNode calls = message.path("tool_calls");
            if (!calls.isArray() || calls.isEmpty()) {
                throw new IllegalStateException("Invalid tool calls");
            }
            if (round == MAX_ROUNDS - 1
                    || totalToolCalls + calls.size() > MAX_TOOL_CALLS) {
                throw new IllegalStateException("AI tool execution limit reached");
            }

            Set<String> idsThisBatch = new HashSet<>();
            for (JsonNode call : calls) {
                validateToolCall(principal, call);
                if (!idsThisBatch.add(call.path("id").asText())) {
                    throw new IllegalArgumentException("Duplicate tool call ID");
                }
            }

            messages.add(message);
            if (turnId != null) {
                turnPersistence.savePendingTools(
                        principal, turnId, messages, round + 1, totalToolCalls);
            }

            for (JsonNode call : calls) {
                String callId = call.path("id").asText();
                String toolName = call.path("function").path("name").asText();
                JsonNode arguments = mapper.readTree(
                        call.path("function").path("arguments").asText());

                renewLeaseIfNeeded(principal, turnId, leaseToken);
                Object result = (principal == null)
                        ? gameToolDispatcher.dispatchStateless(toolName, arguments)
                        : gameToolDispatcher.dispatchAgent(
                                principal, toolName, arguments, callId);
                renewLeaseIfNeeded(principal, turnId, leaseToken);

                if (turnId != null) {
                    turnPersistence.saveToolResult(
                            principal, turnId, callId, result);
                }
                messages.add(Map.of(
                        "role", "tool",
                        "tool_call_id", callId,
                        "content", mapper.writeValueAsString(result)));
                totalToolCalls++;
            }
        }
        throw new IllegalStateException("AI did not complete within round limit");
    }

    private void renewLeaseIfNeeded(
            AgentPrincipal principal, Long turnId, String leaseToken) {
        if (turnId != null) {
            turnPersistence.renewLease(principal, turnId, leaseToken);
        }
    }

    private void validateToolCall(AgentPrincipal principal, JsonNode call) {
        String name = call.path("function").path("name").asText();
        boolean allowed = "roll_dice".equals(name)
                || (principal != null && (
                        "execute_action".equals(name)
                        || "get_character".equals(name)
                        || "get_available_actions".equals(name)));
        if (!"function".equals(call.path("type").asText())
                || !call.path("id").isTextual()
                || call.path("id").asText().isBlank()
                || !allowed) {
            throw new IllegalArgumentException("Unsupported tool call: " + name);
        }
        JsonNode arguments = mapper.readTree(
                call.path("function").path("arguments").asText());
        if (!arguments.isObject()) {
            throw new IllegalArgumentException("Invalid tool arguments");
        }
    }

    private List<Object> diceToolDefinition() {
        return List.of(Map.of(
                "type", "function",
                "function", Map.of(
                        "name", "roll_dice",
                        "description", "Roll dice using a dice expression.",
                        "parameters", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "expression", Map.of(
                                                "type", "string",
                                                "description", "Dice expression such as 1d20+5")),
                                "required", List.of("expression"),
                                "additionalProperties", false))));
    }

    private Map<String, Object> actionToolDefinition() {
        return Map.of(
                "type", "function",
                "function", Map.of(
                        "name", "execute_action",
                        "description", "Execute a learned TRPG action for a character.",
                        "parameters", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "actionId", Map.of("type", "integer"),
                                        "actorId", Map.of("type", "integer"),
                                        "targetId", Map.of("type", "integer")),
                                "required", List.of("actionId", "actorId"),
                                "additionalProperties", false)));
    }

    private Map<String, Object> characterToolDefinition() {
        return Map.of(
                "type", "function",
                "function", Map.of(
                        "name", "get_character",
                        "description", "Get visible character information by ID.",
                        "parameters", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "characterId", Map.of("type", "integer")),
                                "required", List.of("characterId"),
                                "additionalProperties", false)));
    }

    private Map<String, Object> availableActionsToolDefinition() {
        return Map.of(
                "type", "function",
                "function", Map.of(
                        "name", "get_available_actions",
                        "description", "List learned actions for an Agent-controlled character, "
                                + "including resource costs and current affordability.",
                        "parameters", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "actorId", Map.of("type", "integer")),
                                "required", List.of("actorId"),
                                "additionalProperties", false)));
    }
}
