package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.example.demo.Entity.AiTurn;
import com.example.demo.enums.AiTurnStatus;
import com.example.demo.service.ai.AiAgentService;
import com.example.demo.service.ai.AiTurnPersistenceService;
import com.example.demo.service.ai.DeepSeekClient;
import com.example.demo.service.tool.AgentPrincipal;
import com.example.demo.service.tool.GameToolDispatcher;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

class AiTurnRecoveryTest {

    @Test
    void resumesSameCallAfterResultCheckpointFails() {
        ObjectMapper mapper = new ObjectMapper();
        DeepSeekClient client = mock(DeepSeekClient.class);
        GameToolDispatcher dispatcher = mock(GameToolDispatcher.class);
        AiTurnPersistenceService persistence = mock(AiTurnPersistenceService.class);
        AgentPrincipal principal = new AgentPrincipal(1L, 10L);

        JsonNode toolReply = mapper.readTree("""
            {
              "choices": [{
                "finish_reason": "tool_calls",
                "message": {
                  "role": "assistant",
                  "tool_calls": [{
                    "id": "call_123",
                    "type": "function",
                    "function": {
                      "name": "execute_action",
                      "arguments": "{}"
                    }
                  }]
                }
              }]
            }
            """);
        ((ObjectNode) toolReply.path("choices").get(0)
                .path("message").path("tool_calls").get(0)
                .path("function")).put("arguments",
                        mapper.writeValueAsString(Map.of("actionId", 4, "actorId", 17)));

        JsonNode finalReply = mapper.readTree("""
            {
              "choices": [{
                "finish_reason": "stop",
                "message": {"role": "assistant", "content": "攻击已经完成。"}
              }]
            }
            """);
        when(client.chatWithTools(anyList(), anyList(), eq("auto")))
                .thenReturn(toolReply, finalReply);

        AiTurn turn = new AiTurn();
        turn.setId(100L);
        turn.setCampaignId(1L);
        turn.setAgentId(10L);
        turn.setStatus(AiTurnStatus.RUNNING);
        turn.setRoundCount(0);
        turn.setToolCallCount(0);

        when(persistence.createTurn(eq(principal), anyList())).thenReturn(100L);
        when(persistence.acquireLease(principal, 100L))
                .thenReturn("lease-A", "lease-B");
        when(persistence.loadTurn(principal, 100L)).thenReturn(turn);

        // 模拟保存检查点：先写入待执行的工具调用。
        doAnswer(invocation -> {
            List<Object> messages = invocation.getArgument(2);
            turn.setMessagesJson(mapper.writeValueAsString(messages));
            turn.setRoundCount(invocation.getArgument(3));
            turn.setToolCallCount(invocation.getArgument(4));
            turn.setStatus(AiTurnStatus.WAITING_TOOL);
            return null;
        }).when(persistence).savePendingTools(
                eq(principal), eq(100L), anyList(), anyInt(), anyInt());

        List<String> actionResult = List.of("造成5点伤害");
        when(dispatcher.dispatchAgent(
                eq(principal), eq("execute_action"),
                any(JsonNode.class), eq("call_123")))
                .thenReturn(actionResult);

        // 第一次工具结果入库失败；恢复时第二次写入成功。
        AtomicInteger saveAttempts = new AtomicInteger();
        doAnswer(invocation -> {
            if (saveAttempts.getAndIncrement() == 0) {
                throw new IllegalStateException("模拟保存结果失败");
            }
            List<Object> messages = new ArrayList<>(mapper.readValue(
                    turn.getMessagesJson(), new TypeReference<List<Object>>() {}));
            messages.add(Map.of(
                    "role", "tool",
                    "tool_call_id", invocation.getArgument(2),
                    "content", mapper.writeValueAsString(invocation.getArgument(3))));
            turn.setMessagesJson(mapper.writeValueAsString(messages));
            turn.setToolCallCount(turn.getToolCallCount() + 1);
            turn.setStatus(AiTurnStatus.RUNNING);
            return null;
        }).when(persistence).saveToolResult(
                eq(principal), eq(100L), eq("call_123"), eq(actionResult));

        doAnswer(invocation -> {
            turn.setFinalResponse(invocation.getArgument(2));
            turn.setStatus(AiTurnStatus.COMPLETED);
            return null;
        }).when(persistence).completeTurn(
                eq(principal), eq(100L), anyString());

        AiAgentService service = new AiAgentService(
                client, dispatcher, mapper, persistence);

        // 第一次：行动已被分发，写回 AiTurn 时失败。
        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> service.chatAsAgent(principal, "发动攻击"));
        assertEquals("模拟保存结果失败", error.getMessage());
        assertEquals(AiTurnStatus.WAITING_TOOL, turn.getStatus());
        verify(client, times(1)).chatWithTools(anyList(), anyList(), eq("auto"));

        // 第二次：直接恢复原来的 Turn，不让模型重新生成一次攻击指令。
        String answer = service.resumeTurn(principal, 100L);
        assertEquals("攻击已经完成。", answer);
        assertEquals(AiTurnStatus.COMPLETED, turn.getStatus());
        assertEquals(1, turn.getToolCallCount());
        assertEquals(2, saveAttempts.get());

        // 确认两次分发都使用了同一个执行 ID；真正去重由 IdempotentActionService 保证。
        verify(dispatcher, times(2)).dispatchAgent(
                eq(principal), eq("execute_action"),
                any(JsonNode.class), eq("call_123"));
        verify(client, times(2)).chatWithTools(anyList(), anyList(), eq("auto"));

        // 恢复后 AiTurn 中只有一条该工具的结果消息。
        JsonNode history = mapper.readTree(turn.getMessagesJson());
        int resultMessages = 0;
        for (JsonNode message : history) {
            if ("tool".equals(message.path("role").asText())
                    && "call_123".equals(message.path("tool_call_id").asText())) {
                resultMessages++;
            }
        }
        assertEquals(1, resultMessages);
    }
}
