package com.example.demo.service.tool;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

@Component
public class GameToolDispatcher {

    private final GameToolService gameToolService;

    public GameToolDispatcher(GameToolService gameToolService) {
        this.gameToolService = gameToolService;
    }

    public Object dispatch(
            ToolContext context,
            String toolName,
            JsonNode arguments) {

        if (context == null
                || context.campaignId() == null
                || context.userId() == null
                || context.callerType() == null) {
            throw new IllegalArgumentException(
                    "Invalid tool context");
        }

        // 目前仅接通同步 HTTP 请求中的真人身份。
        // AI Agent 的授权将在后续单独实现。
        if (context.callerType() != ToolCallerType.HUMAN) {
            throw new UnsupportedOperationException(
                    "AI agent authorization is not implemented");
        }

        if (toolName == null || arguments == null
                || !arguments.isObject()) {
            throw new IllegalArgumentException(
                    "Invalid tool call");
        }

        return switch (toolName) {

            case "roll_dice" -> {
                String expression = requireText(
                        arguments, "expression");

                yield gameToolService.rollDice(
                        context, expression);
            }

            case "execute_action" -> {
                Long actionId = requireLong(
                        arguments, "actionId");

                Long actorId = requireLong(
                        arguments, "actorId");

                Long targetId = optionalLong(
                        arguments, "targetId");

                yield gameToolService.executeAction(
                        actionId, actorId, targetId);
            }

            default -> throw new IllegalArgumentException(
                    "Unknown tool: " + toolName);
        };
    }

    private String requireText(JsonNode args, String field) {
        JsonNode value = args.get(field);

        if (value == null || !value.isTextual()
                || value.asText().isBlank()) {
            throw new IllegalArgumentException(
                    "Invalid field: " + field);
        }

        return value.asText();
    }

    private Long requireLong(JsonNode args, String field) {
        JsonNode value = args.get(field);

        if (value == null || !value.isIntegralNumber()
                || !value.canConvertToLong()
                || value.longValue() <= 0) {
            throw new IllegalArgumentException(
                    "Invalid field: " + field);
        }

        return value.longValue();
    }

    private Long optionalLong(JsonNode args, String field) {
        JsonNode value = args.get(field);

        if (value == null || value.isNull()) {
            return null;
        }

        return requireLong(args, field);
    }
}