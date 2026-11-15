
package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.SocketTimeoutException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.example.demo.service.ai.AiAgentService;
import com.example.demo.service.ai.DeepSeekClient;
import com.example.demo.service.tool.AgentPrincipal;
import com.example.demo.service.tool.GameToolDispatcher;

import tools.jackson.databind.ObjectMapper;

class AiNetworkRetryTest {

    @Test
    void shouldRetryHttpWithoutExecutingActionTwice() {

        // 1. 创建模拟 HTTP 服务器
        RestClient.Builder builder = RestClient.builder();

        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        DeepSeekClient client = new DeepSeekClient(
                builder, "fake-api-key", "deepseek-chat");

        GameToolDispatcher dispatcher =
                mock(GameToolDispatcher.class);

        AiAgentService service = new AiAgentService(
                client, dispatcher, new ObjectMapper());

        AgentPrincipal principal =
                new AgentPrincipal(1L, 2L);

        String url =
                "https://api.deepseek.com/chat/completions";

        // 2. 第一次 HTTP 请求：模型决定攻击
        server.expect(requestTo(url))
                .andRespond(withSuccess("""
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
                              "arguments":
                                "{\\"actionId\\":4,\\"actorId\\":17}"
                            }
                          }]
                        }
                      }]
                    }
                    """, MediaType.APPLICATION_JSON));

        // 3. 第二次 HTTP 请求：模拟超时
        // 此时 Java 已经执行了攻击
        server.expect(requestTo(url))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "call_123")))
                .andRespond(withException(
                        new SocketTimeoutException("Simulated timeout")));

        // 4. 第三次 HTTP 请求：相同上下文重试成功
        server.expect(requestTo(url))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "call_123")))
                .andRespond(withSuccess("""
                    {
                      "choices": [{
                        "finish_reason": "stop",
                        "message": {
                          "role": "assistant",
                          "content": "攻击命中，造成5点伤害。"
                        }
                      }]
                    }
                    """, MediaType.APPLICATION_JSON));

        // 5. 模拟游戏工具执行成功
        when(dispatcher.dispatchAgent(
                eq(principal),
                eq("execute_action"),
                any(),
                anyString()
        )).thenReturn(List.of("攻击命中，造成5点伤害"));

        // 6. 运行真正的 AiAgentService
        String answer = service.chatAsAgent(
                principal, "攻击强盗");

        assertEquals("攻击命中，造成5点伤害。", answer);

        // 7. 验证 HTTP 一共请求了三次
        server.verify();

        // 8. 验证 Action 只执行了一次
        verify(dispatcher, times(1))
                .dispatchAgent(
                        eq(principal),
                        eq("execute_action"),
                        any(),
                        anyString()
                );
    }
}
