
package com.example.demo.service.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.demo.service.DiceService;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class DeepSeekDiceService {

    private final DeepSeekClient deepSeekClient;
    private final DiceService diceService;
    private final ObjectMapper objectMapper;

    public DeepSeekDiceService(
            DeepSeekClient deepSeekClient,
            DiceService diceService,
            ObjectMapper objectMapper) {

        this.deepSeekClient = deepSeekClient;
        this.diceService = diceService;
        this.objectMapper = objectMapper;
    }

    public String ask(String prompt) {

        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException(
                    "Prompt cannot be empty");
        }

        // 1. 定义本次对话允许使用的工具
        List<Object> tools = List.of(
                Map.of(
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
                                        "required", List.of("expression")))));

        // 2. 构造对话消息
        List<Object> messages = new ArrayList<>();

        messages.add(Map.of(
                "role", "system",
                "content",
                "When asked to roll dice, use roll_dice. "
                        + "Never invent dice results."));

        messages.add(Map.of(
                "role", "user",
                "content", prompt));

        // 3. 第一次调用 DeepSeek
        JsonNode response = deepSeekClient.chatWithTools(
        messages, tools, "required");

        JsonNode choice = response.path("choices").get(0);
        String finishReason = choice.path("finish_reason").asText();

        // 模型直接返回文本
        if ("stop".equals(finishReason)) {
            return requireText(choice);
        }

        if (!"tool_calls".equals(finishReason)) {
            throw new IllegalStateException(
                    "Unexpected finish reason: " + finishReason);
        }

        // 4. 读取模型提出的工具调用
        JsonNode assistantMessage = choice.path("message");
        JsonNode toolCalls = assistantMessage.path("tool_calls");

        // 第一版仅支持一轮中的一个工具调用
        if (!toolCalls.isArray() || toolCalls.size() != 1) {
            throw new IllegalStateException(
                    "Expected exactly one tool call");
        }

        JsonNode call = toolCalls.get(0);
        String callId = call.path("id").asText();
        String toolName = call.path("function").path("name").asText();

        if (callId.isBlank()
                || !"roll_dice".equals(toolName)) {
            throw new IllegalArgumentException(
                    "Unsupported tool call");
        }

        try {
            // 5. 解析模型生成的参数
            String argumentJson = call.path("function")
                    .path("arguments").asText();

            JsonNode arguments = objectMapper.readTree(argumentJson);

            JsonNode expressionNode = arguments.path("expression");

            if (!expressionNode.isTextual()
                    || expressionNode.asText().isBlank()) {
                throw new IllegalArgumentException(
                        "Invalid dice expression");
            }

            // 6. 真正执行骰子
            var diceResult = diceService.roll(expressionNode.asText());

            // 7. 添加原始 assistant 工具请求
            messages.add(assistantMessage);

            // 8. 将骰子结果关联回对应的调用 ID
            messages.add(Map.of(
                    "role", "tool",
                    "tool_call_id", callId,
                    "content",
                    objectMapper.writeValueAsString(diceResult)));

        } catch (JacksonException e) {
            throw new IllegalArgumentException(
                    "Invalid tool arguments or result JSON", e);
        }

        // 9. 第二次调用 DeepSeek，让它根据骰子结果回答
        JsonNode finalResponse = deepSeekClient.chatWithTools(
        messages, tools, "none");

        JsonNode finalChoice = finalResponse.path("choices").get(0);

        if (!"stop".equals(
                finalChoice.path("finish_reason").asText())) {
            throw new IllegalStateException(
                    "AI did not complete its final response");
        }

        return requireText(finalChoice);
    }

    private String requireText(JsonNode choice) {

        JsonNode content = choice.path("message").path("content");

        if (!content.isTextual()
                || content.asText().isBlank()) {
            throw new IllegalStateException(
                    "AI returned no text");
        }

        return content.asText();
    }
}
