package com.example.demo.service.ai;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import tools.jackson.databind.JsonNode;

@Component
public class DeepSeekClient {

        private final RestClient restClient;
        private final String apiKey;
        private final String model;

        public DeepSeekClient(
                        RestClient.Builder builder,
                        @Value("${ai.deepseek.api-key:}") String apiKey,
                        @Value("${ai.deepseek.model:deepseek-flash}") String model) {

                this.apiKey = apiKey;
                this.model = model;

                this.restClient = builder
                                .baseUrl("https://api.deepseek.com")
                                .build();
        }

        public String generateText(String prompt) {

                if (apiKey == null || apiKey.isBlank()) {
                        throw new IllegalStateException(
                                        "DEEPSEEK_API_KEY is not configured");
                }

                if (model == null || model.isBlank()) {
                        throw new IllegalStateException(
                                        "DeepSeek model is not configured");
                }

                if (prompt == null || prompt.isBlank()) {
                        throw new IllegalArgumentException(
                                        "Prompt cannot be empty");
                }

                ChatRequest request = new ChatRequest(
                                model,
                                List.of(new ChatMessage("user", prompt)),
                                false);

                JsonNode response;

                try {
                        response = restClient.post()
                                        .uri("/chat/completions")
                                        .headers(headers -> headers.setBearerAuth(apiKey))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .body(request)
                                        .retrieve()
                                        .body(JsonNode.class);

                } catch (RestClientResponseException e) {

                        int status = e.getStatusCode().value();

                        if (status == 401) {
                                throw new IllegalStateException(
                                                "DeepSeek API authentication failed", e);
                        }

                        if (status == 429) {
                                throw new IllegalStateException(
                                                "DeepSeek API rate or quota limit reached", e);
                        }

                        throw new IllegalStateException(
                                        "DeepSeek API returned HTTP " + status, e);

                } catch (ResourceAccessException e) {

                        throw new IllegalStateException(
                                        "DeepSeek connection or timeout error", e);

                } catch (RestClientException e) {

                        throw new IllegalStateException(
                                        "DeepSeek API request failed", e);
                }

                if (response == null) {
                        throw new IllegalStateException(
                                        "DeepSeek returned an empty response");
                }

                JsonNode choices = response.path("choices");

                if (!choices.isArray() || choices.isEmpty()) {
                        throw new IllegalStateException(
                                        "DeepSeek response has no choices");
                }

                JsonNode firstChoice = choices.get(0);

                String finishReason = firstChoice
                                .path("finish_reason")
                                .asText();

                if (!"stop".equals(finishReason)) {
                        throw new IllegalStateException(
                                        "DeepSeek response did not finish normally: "
                                                        + finishReason);
                }

                JsonNode content = firstChoice
                                .path("message")
                                .path("content");

                if (!content.isTextual()
                                || content.asText().isBlank()) {
                        throw new IllegalStateException(
                                        "DeepSeek returned no text");
                }

                return content.asText();
        }

        public JsonNode chatWithTools(
                        List<Object> messages,
                        List<Object> tools,
                        String toolChoice) {

                if (apiKey == null || apiKey.isBlank()) {
                        throw new IllegalStateException(
                                        "DEEPSEEK_API_KEY is not configured");
                }

                try {
                        JsonNode response = restClient.post()
                                        .uri("/chat/completions")
                                        .headers(h -> h.setBearerAuth(apiKey))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .body(Map.of(
                                                        "model", model,
                                                        "messages", messages,
                                                        "tools", tools,
                                                        "tool_choice", toolChoice,
                                                        "thinking", Map.of("type", "disabled"),
                                                        "stream", false))
                                        .retrieve()
                                        .body(JsonNode.class);

                        if (response == null
                                        || !response.path("choices").isArray()
                                        || response.path("choices").isEmpty()) {
                                throw new IllegalStateException(
                                                "Invalid DeepSeek response");
                        }

                        return response;

                } catch (RestClientException e) {
                        throw new IllegalStateException(
                                        "DeepSeek tool request failed", e);
                }
        }

        private record ChatMessage(
                        String role,
                        String content) {
        }

        private record ChatRequest(
                        String model,
                        List<ChatMessage> messages,
                        boolean stream) {
        }
}
