
package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;

class RestClientPostPracticeTest {

    @Test
    void shouldSendJsonPostRequest() {

        RestClient.Builder builder = RestClient.builder();

        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        server.expect(requestTo("https://example.com/message"))
                .andExpect(method(
                        org.springframework.http.HttpMethod.POST))
                .andExpect(content().contentType(
                        MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                    {
                      "message": "莉莉丝向守卫打招呼"
                    }
                    """))
                .andRespond(withSuccess(
                        """
                        {
                          "reply": "守卫向莉莉丝点了点头。"
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        RestClient client = builder.build();

        Map<String, String> request = Map.of(
                "message", "莉莉丝向守卫打招呼"
        );

        JsonNode response=client.post()
        .uri("https://example.com/message")
        .contentType(MediaType.APPLICATION_JSON)
        .body(request)
        .retrieve()
        .body(JsonNode.class);

         String reply = response.path("reply").asText();

        assertEquals("守卫向莉莉丝点了点头。", reply);

        server.verify();
    }
}
