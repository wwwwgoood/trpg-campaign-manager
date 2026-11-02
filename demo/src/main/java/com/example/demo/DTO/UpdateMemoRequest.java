package com.example.demo.dto;

import com.example.demo.MemoCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateMemoRequest(
    @NotBlank 
    String name,
    @NotNull 
    MemoCategory category,
    String description
) {
}