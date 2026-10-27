package com.example.demo.DTO;

import com.example.demo.MemoCategory;

public record MemoResponse(
    Long id,
    Long campaignId,
    String name,
    MemoCategory category,
    String description
) {
}