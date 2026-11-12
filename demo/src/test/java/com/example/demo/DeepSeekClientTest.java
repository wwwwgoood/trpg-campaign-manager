
package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.example.demo.service.ai.DeepSeekClient;

class DeepSeekClientTest {

    @Test
    void shouldGenerateText() {

        RestClient.Builder builder = RestClient.builder();

        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        server.expect(requestTo(
                "https://api.deepseek.com/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(
                        MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                    {
                      "model": "deepseek-flash",
                      "messages": [
                        {
                          "role": "user",
                          "content": "描述莉莉丝进入酒馆"
                        }
                      ],
                      "stream": false
                    }
                    """))
                .andRespond(withSuccess("""
                    {
                      "choices": [
                        {
                          "finish_reason": "stop",
                          "message": {
                            "role": "assistant",
                            "content": "莉莉丝走进了酒馆。"
                          }
                        }
                      ]
                    }
                    """, MediaType.APPLICATION_JSON));

        DeepSeekClient client = new DeepSeekClient(
                builder,
                "fake-api-key",
                "deepseek-flash"
        );

        String result = client.generateText(
                "描述莉莉丝进入酒馆");

        assertEquals("莉莉丝走进了酒馆。", result);

        server.verify();
    }

    @Test
    void shouldHandleUnauthorized() {

        RestClient.Builder builder = RestClient.builder();

        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        server.expect(requestTo(
                "https://api.deepseek.com/chat/completions"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        DeepSeekClient client = new DeepSeekClient(
                builder,
                "fake-api-key",
                "deepseek-flash"
        );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> client.generateText("你好")
        );

        assertEquals(
                "DeepSeek API authentication failed",
                exception.getMessage()
        );

        server.verify();
    }
}
