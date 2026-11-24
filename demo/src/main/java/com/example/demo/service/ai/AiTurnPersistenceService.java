
package com.example.demo.service.ai;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.Entity.AiTurn;
import com.example.demo.enums.AiTurnStatus;
import com.example.demo.repository.AiTurnRepository;
import com.example.demo.service.tool.AgentPrincipal;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class AiTurnPersistenceService {

    private final AiTurnRepository turnRepository;
    private final ObjectMapper mapper;
    private final JdbcTemplate jdbc;

    public AiTurnPersistenceService(
            AiTurnRepository turnRepository,
            ObjectMapper mapper,
            JdbcTemplate jdbc) {

        this.turnRepository = turnRepository;
        this.mapper = mapper;
        this.jdbc = jdbc;
    }

    public String acquireLease(
            AgentPrincipal principal,
            Long turnId) {

        if (principal == null || turnId == null || turnId <= 0) {
            throw new IllegalArgumentException(
                    "Invalid lease request");
        }

        // 每次领取生成不同的身份标识
        String token = UUID.randomUUID().toString();

        int updated = jdbc.update(
                """
                        UPDATE ai_turn
                        SET lease_owner = ?,
                            lease_until = CURRENT_TIMESTAMP
                                          + INTERVAL '10 minutes',
                            version = version + 1
                        WHERE id = ?
                          AND campaign_id = ?
                          AND agent_id = ?
                          AND status IN ('RUNNING', 'WAITING_TOOL')
                          AND (
                              lease_until IS NULL
                              OR lease_until < CURRENT_TIMESTAMP
                          )
                        """,
                token,
                turnId,
                principal.campaignId(),
                principal.agentId());

        if (updated != 1) {
            throw new IllegalStateException(
                    "AI turn is not available for execution");
        }

        return token;
    }

    public void releaseLease(
            AgentPrincipal principal,
            Long turnId,
            String token) {

        if (principal == null
                || turnId == null || turnId <= 0
                || token == null || token.isBlank()) {

            throw new IllegalArgumentException(
                    "Invalid lease release");
        }

        jdbc.update(
                """
                        UPDATE ai_turn
                        SET lease_owner = NULL,
                            lease_until = NULL,
                            version = version + 1
                        WHERE id = ?
                          AND campaign_id = ?
                          AND agent_id = ?
                          AND lease_owner = ?
                        """,
                turnId,
                principal.campaignId(),
                principal.agentId(),
                token);
    }

    // 创建新回合，并持久化初始消息历史
    @Transactional
    public Long createTurn(
            AgentPrincipal principal,
            List<Object> messages) {

        if (principal == null || messages == null) {
            throw new IllegalArgumentException(
                    "Principal and messages are required");
        }

        AiTurn turn = new AiTurn();

        turn.setCampaignId(principal.campaignId());
        turn.setAgentId(principal.agentId());
        turn.setStatus(AiTurnStatus.RUNNING);
        turn.setMessagesJson(
                mapper.writeValueAsString(messages));

        turn.setRoundCount(0);
        turn.setToolCallCount(0);

        return turnRepository.save(turn).getId();
    }

    // 保存模型提出的工具调用，并设置为等待执行
    @Transactional
    public void savePendingTools(
            AgentPrincipal principal,
            Long turnId,
            List<Object> messages,
            int roundCount,
            int toolCallCount) {

        AiTurn turn = requireTurn(principal, turnId);

        if (turn.getStatus() != AiTurnStatus.RUNNING) {
            throw new IllegalStateException(
                    "Turn is not running");
        }

        if (messages == null
                || roundCount < 0
                || toolCallCount < 0) {
            throw new IllegalArgumentException(
                    "Invalid turn snapshot");
        }

        turn.setMessagesJson(
                mapper.writeValueAsString(messages));

        turn.setRoundCount(roundCount);
        turn.setToolCallCount(toolCallCount);

        turn.setStatus(AiTurnStatus.WAITING_TOOL);
    }

    // 读取时始终检查战役和 Agent 归属
    @Transactional(readOnly = true)
    public AiTurn loadTurn(
            AgentPrincipal principal,
            Long turnId) {

        return requireTurn(principal, turnId);
    }

    private AiTurn requireTurn(
            AgentPrincipal principal,
            Long turnId) {

        if (principal == null
                || turnId == null
                || turnId <= 0) {
            throw new IllegalArgumentException(
                    "Invalid turn identity");
        }

        AiTurn turn = turnRepository.findById(turnId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "AI turn not found"));

        if (!principal.campaignId().equals(turn.getCampaignId())
                || !principal.agentId().equals(turn.getAgentId())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Cannot access this AI turn");
        }

        return turn;
    }

    @Transactional
    public void saveToolResult(
            AgentPrincipal principal,
            Long turnId,
            String callId,
            Object result) {

        AiTurn turn = requireTurn(principal, turnId);

        if (callId == null || callId.isBlank()) {
            throw new IllegalArgumentException("Tool call ID is required");
        }

        List<Object> messages = new ArrayList<>(
                mapper.readValue(
                        turn.getMessagesJson(),
                        new TypeReference<List<Object>>() {
                        }));

        // 1. 找到最近一轮模型提出的工具调用
        int assistantIndex = -1;
        Set<String> expectedIds = new HashSet<>();

        for (int i = messages.size() - 1; i >= 0; i--) {
            JsonNode node = mapper.valueToTree(messages.get(i));

            if ("assistant".equals(node.path("role").asText())
                    && node.path("tool_calls").isArray()) {

                assistantIndex = i;

                for (JsonNode call : node.path("tool_calls")) {
                    expectedIds.add(call.path("id").asText());
                }

                break;
            }
        }

        if (assistantIndex < 0 || !expectedIds.contains(callId)) {
            throw new IllegalArgumentException(
                    "Tool call does not belong to the pending batch");
        }

        // 2. 找出已经保存结果的工具
        Set<String> completedIds = new HashSet<>();
        String resultJson = mapper.writeValueAsString(result);

        for (int i = assistantIndex + 1; i < messages.size(); i++) {
            JsonNode node = mapper.valueToTree(messages.get(i));

            if ("tool".equals(node.path("role").asText())) {
                String completedId = node.path("tool_call_id").asText();

                completedIds.add(completedId);

                // 相同结果的重复保存，直接返回
                if (callId.equals(completedId)) {
                    if (!resultJson.equals(
                            node.path("content").asText())) {
                        throw new IllegalStateException(
                                "Tool result conflicts with saved result");
                    }
                    return;
                }
            }
        }

        if (turn.getStatus() != AiTurnStatus.WAITING_TOOL) {
            throw new IllegalStateException(
                    "Turn is not waiting for tools");
        }

        // 3. 将本次工具结果加入消息历史
        messages.add(Map.of(
                "role", "tool",
                "tool_call_id", callId,
                "content", resultJson));

        completedIds.add(callId);

        turn.setMessagesJson(
                mapper.writeValueAsString(messages));

        turn.setToolCallCount(turn.getToolCallCount() + 1);

        // 4. 所有工具都有结果后，才允许继续调用模型
        if (completedIds.containsAll(expectedIds)) {
            turn.setStatus(AiTurnStatus.RUNNING);
        }
    }

    @Transactional
    public void completeTurn(
            AgentPrincipal principal,
            Long turnId,
            String finalResponse) {

        AiTurn turn = requireTurn(principal, turnId);

        if (turn.getStatus() != AiTurnStatus.RUNNING) {
            throw new IllegalStateException(
                    "Turn cannot be completed in current state");
        }

        if (finalResponse == null || finalResponse.isBlank()) {
            throw new IllegalArgumentException(
                    "Final response cannot be empty");
        }

        List<Object> messages = new ArrayList<>(
                mapper.readValue(
                        turn.getMessagesJson(),
                        new TypeReference<List<Object>>() {
                        }));

        messages.add(Map.of(
                "role", "assistant",
                "content", finalResponse));

        turn.setMessagesJson(
                mapper.writeValueAsString(messages));

        turn.setFinalResponse(finalResponse);
        turn.setStatus(AiTurnStatus.COMPLETED);
    }

    public void renewLease(
            AgentPrincipal principal,
            Long turnId,
            String token) {

        if (principal == null
                || turnId == null || turnId <= 0
                || token == null || token.isBlank()) {
            throw new IllegalArgumentException(
                    "Invalid lease renewal request");
        }

        int updated = jdbc.update(
                """
                        UPDATE ai_turn
                        SET lease_until = CURRENT_TIMESTAMP
                                          + INTERVAL '10 minutes',
                            version = version + 1
                        WHERE id = ?
                          AND campaign_id = ?
                          AND agent_id = ?
                          AND lease_owner = ?
                          AND lease_until > CURRENT_TIMESTAMP
                          AND status IN ('RUNNING', 'WAITING_TOOL')
                        """,
                turnId,
                principal.campaignId(),
                principal.agentId(),
                token);

        if (updated != 1) {
            throw new IllegalStateException(
                    "AI turn lease has expired or was lost");
        }
    }
}
