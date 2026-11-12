package com.example.demo.service.tool;

public record HumanPrincipal(
        Long campaignId,
        Long userId
) implements ExecutionPrincipal {

    public HumanPrincipal {
        if (campaignId == null || campaignId <= 0
                || userId == null || userId <= 0) {

            throw new IllegalArgumentException(
                    "Invalid human principal");
        }
    }
}