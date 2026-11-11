package com.example.demo.dto;

import com.example.demo.MemoCategory;

public record MemoResponse(
    Long id,
    Long campaignId,
    String name,
    MemoCategory category,
    String description,
    boolean isPrivate,
Long ownerMemberId
) {
}