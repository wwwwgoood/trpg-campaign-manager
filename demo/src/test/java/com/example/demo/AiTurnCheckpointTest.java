package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import com.example.demo.service.ai.AiAgentService;
import com.example.demo.service.ai.AiTurnPersistenceService;
import com.example.demo.service.ai.DeepSeekClient;
import com.example.demo.service.tool.AgentPrincipal;
import com.example.demo.service.tool.GameToolDispatcher;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

class AiTurnCheckpointTest {

    @Test
    void savesToolCallBeforeExecutingAction() {
        ObjectMapper mapper = new ObjectMapper();
        DeepSeekClient client = mock(DeepSeekClient.class);
        GameToolDispatcher dispatcher = mock(GameToolDispatcher.class);
        AiTurnPersistenceService persistence = mock(AiTurnPersistenceService.class);
        AgentPrincipal principal = new AgentPrincipal(1L, 10L);

        // 模拟 DeepSeek 第一次返回一个会改变状态的工具调用。
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
        ObjectNode function = (ObjectNode) toolReply.path("choices").get(0)
                .path("message").path("tool_calls").get(0).path("function");
        function.put("arguments", mapper.writeValueAsString(
                Map.of("actionId", 4, "actorId", 17)));

        // 第二次响应：模型收到工具结果并完成回复。
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
        when(persistence.createTurn(eq(principal), anyList()))
                .thenReturn(100L);
        when(persistence.acquireLease(principal, 100L))
                .thenReturn("lease-A");
        when(dispatcher.dispatchAgent(eq(principal), eq("execute_action"),
                any(JsonNode.class), eq("call_123")))
                .thenReturn(List.of("造成5点伤害"));

        AiAgentService service = new AiAgentService(
                client, dispatcher, mapper, persistence);

        String answer = service.chatAsAgent(principal, "角色17发动攻击");
        assertEquals("攻击已经完成。", answer);

        // 验证跨组件调用顺序；不验证真实数据库事务提交。
        InOrder order = inOrder(persistence, client, dispatcher);
        order.verify(persistence).createTurn(eq(principal), anyList());
        order.verify(persistence).acquireLease(principal, 100L);
        order.verify(client).chatWithTools(anyList(), anyList(), eq("auto"));
        order.verify(persistence).savePendingTools(
                eq(principal), eq(100L), anyList(), eq(1), eq(0));
        order.verify(dispatcher).dispatchAgent(eq(principal),
                eq("execute_action"), any(JsonNode.class), eq("call_123"));
        order.verify(persistence).saveToolResult(
                principal, 100L, "call_123", List.of("造成5点伤害"));
        order.verify(client).chatWithTools(anyList(), anyList(), eq("auto"));
        order.verify(persistence).completeTurn(principal, 100L, "攻击已经完成。");
        order.verify(persistence).releaseLease(principal, 100L, "lease-A");
    }
}
