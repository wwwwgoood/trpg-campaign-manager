package com.example.demo.service.tool;

public record ToolContext(
        Long campaignId,
        Long userId,
        ToolCallerType callerType
) {}