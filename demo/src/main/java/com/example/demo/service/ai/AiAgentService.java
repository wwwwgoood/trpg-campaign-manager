
package com.example.demo.service.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.demo.service.tool.AgentPrincipal;
import com.example.demo.service.tool.GameToolDispatcher;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class AiAgentService {

        private static final int MAX_ROUNDS = 4;
        private static final int MAX_TOOL_CALLS = 8;

        private final DeepSeekClient deepSeekClient;
        private final GameToolDispatcher gameToolDispatcher;
        private final ObjectMapper mapper;

        public AiAgentService(
                        DeepSeekClient deepSeekClient,
                        GameToolDispatcher gameToolDispatcher,
                        ObjectMapper mapper) {

                this.deepSeekClient = deepSeekClient;
                this.gameToolDispatcher = gameToolDispatcher;
                this.mapper = mapper;
        }

        private List<Object> diceToolDefinition() {

                return List.of(Map.of(
                                "type", "function",
                                "function", Map.of(
                                                "name", "roll_dice",
                                                "description",
                                                "Roll dice using a dice expression.",
                                                "parameters", Map.of(
                                                                "type", "object",
                                                                "properties", Map.of(
                                                                                "expression", Map.of(
                                                                                                "type", "string",
                                                                                                "description",
                                                                                                "Dice expression such as 1d20+5")),
                                                                "required", List.of("expression"),
                                                                "additionalProperties", false))));
        }

        public String chat(String prompt) {
                return chatInternal(null, prompt);
        }

        public String chatAsAgent(
                        AgentPrincipal principal,
                        String prompt) {

                if (principal == null) {
                        throw new IllegalArgumentException(
                                        "Agent principal is required");
                }

                // 在请求 DeepSeek 之前验证启动权限
                gameToolDispatcher.requireAgentLaunch(principal);

                return chatInternal(principal, prompt);
        }

        private String chatInternal(
                        AgentPrincipal principal,
                        String prompt) {

                if (prompt == null || prompt.isBlank()) {
                        throw new IllegalArgumentException(
                                        "Prompt cannot be empty");
                }

                List<Object> messages = new ArrayList<>();

                messages.add(Map.of(
                                "role", "system",
                                "content",
                                "You are a TRPG assistant. "
                                                + "Use tools when necessary. "
                                                + "Never invent dice results or action outcomes."));

                messages.add(Map.of(
                                "role", "user",
                                "content", prompt));

                List<Object> tools = new ArrayList<>(diceToolDefinition());

                // 只有经过授权的 Agent 才能获得行动工具定义
                if (principal != null) {
                        tools.add(actionToolDefinition());
                }

                int totalToolCalls = 0;

                for (int round = 0; round < MAX_ROUNDS; round++) {

                        JsonNode response = deepSeekClient.chatWithTools(
                                        List.copyOf(messages),
                                        tools,
                                        "auto");

                        JsonNode choice = response.path("choices").get(0);

                        String finishReason = choice.path("finish_reason").asText();

                        JsonNode message = choice.path("message");

                        if ("stop".equals(finishReason)) {
                                JsonNode content = message.path("content");

                                if (!content.isTextual()
                                                || content.asText().isBlank()) {
                                        throw new IllegalStateException(
                                                        "AI returned no text");
                                }

                                return content.asText();
                        }

                        if (!"tool_calls".equals(finishReason)) {
                                throw new IllegalStateException(
                                                "Unexpected finish reason: "
                                                                + finishReason);
                        }

                        JsonNode calls = message.path("tool_calls");

                        if (!calls.isArray() || calls.isEmpty()) {
                                throw new IllegalStateException(
                                                "Invalid tool calls");
                        }

                        if (round == MAX_ROUNDS - 1
                                        || totalToolCalls + calls.size() > MAX_TOOL_CALLS) {
                                throw new IllegalStateException(
                                                "AI tool execution limit reached");
                        }

                        // 在执行之前检查全部工具名称
                        for (JsonNode call : calls) {

                                String toolName = call.path("function")
                                                .path("name").asText();

                                boolean permitted = "roll_dice".equals(toolName)
                                                || (principal != null
                                                                && "execute_action".equals(toolName));

                                if (!call.path("id").isTextual()
                                                || call.path("id").asText().isBlank()
                                                || !"function".equals(
                                                                call.path("type").asText())
                                                || !permitted) {
                                        throw new IllegalArgumentException(
                                                        "Unsupported tool call: " + toolName);
                                }
                        }

                        // 保存 DeepSeek 的工具调用消息
                        messages.add(message);

                        for (JsonNode call : calls) {

                                String callId = call.path("id").asText();

                                String toolName = call.path("function")
                                                .path("name").asText();

                                JsonNode arguments = mapper.readTree(
                                                call.path("function")
                                                                .path("arguments").asText());

                                if (!arguments.isObject()) {
                                        throw new IllegalArgumentException(
                                                        "Invalid tool arguments");
                                }

                                Object result;

                                if (principal == null) {
                                        // 普通聊天：只能使用无状态工具
                                        result = gameToolDispatcher.dispatchStateless(
                                                        toolName, arguments);
                                } else {
                                        // AI Agent：经过工具分发器进行授权
                                        result = gameToolDispatcher.dispatchAgent(
                                                        principal, toolName, arguments);
                                }

                                messages.add(Map.of(
                                                "role", "tool",
                                                "tool_call_id", callId,
                                                "content",
                                                mapper.writeValueAsString(result)));

                                totalToolCalls++;
                        }
                }

                throw new IllegalStateException(
                                "AI did not complete within round limit");
        }

        private Map<String, Object> actionToolDefinition() {
                return Map.of(
                                "type", "function",
                                "function", Map.of(
                                                "name", "execute_action",
                                                "description",
                                                "Execute a learned TRPG action for a character.",
                                                "parameters", Map.of(
                                                                "type", "object",
                                                                "properties", Map.of(
                                                                                "actionId", Map.of("type", "integer"),
                                                                                "actorId", Map.of("type", "integer"),
                                                                                "targetId", Map.of("type", "integer")),
                                                                "required", List.of("actionId", "actorId"),
                                                                "additionalProperties", false)));
        }
}
