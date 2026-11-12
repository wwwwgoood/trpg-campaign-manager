
package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

class RestClientPracticeTest {

    @Test
    void shouldReadHttpResponse() {

        // 1. 创建客户端配置
        RestClient.Builder builder = RestClient.builder();

        // 2. 创建模拟 HTTP 服务器
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        // 3. 指定服务器应该返回的内容
        server.expect(requestTo("https://example.com/hello"))
                .andRespond(withSuccess(
                        "Hello from server",
                        MediaType.TEXT_PLAIN
                ));

        // 4. 创建真正的 RestClient
        RestClient client = builder.build();
        String result;
        try{
            result = client.get()
            .uri("https://example.com/hello")
            .retrieve()
            .body(String.class);
        }catch(RestClientException e)
        {
            throw new IllegalStateException(
                    "AI API request failed", e);
        }
        

        // 6. 验证结果
        assertEquals("Hello from server", result);

        server.verify();
    }
}
