package com.example.demo.dto;

import com.example.demo.MemoCategory;

import jakarta.validation.constraints.NotBlank;

public record CreateMemoRequest(
        @NotBlank String name,
        MemoCategory category,
        String description,
        boolean isPrivate
) {}
