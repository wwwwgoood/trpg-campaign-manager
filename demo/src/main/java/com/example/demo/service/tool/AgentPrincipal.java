package com.example.demo.service.tool;

public record AgentPrincipal(
        Long campaignId,
        Long agentId
) implements ExecutionPrincipal {

    public AgentPrincipal {
        if (campaignId == null || campaignId <= 0
                || agentId == null || agentId <= 0) {

            throw new IllegalArgumentException(
                    "Invalid agent principal");
        }
    }
}