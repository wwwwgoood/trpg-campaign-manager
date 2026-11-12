package com.example.demo.service.ai;

public record AiExecutionContext(
        Long campaignId,
        Long agentId
) {
    public AiExecutionContext {
        if (campaignId == null || agentId == null
                || campaignId <= 0 || agentId <= 0) {
            throw new IllegalArgumentException(
                    "Invalid AI execution context");
        }
    }
}