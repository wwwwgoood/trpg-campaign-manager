package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCampaignRequest(
    @NotBlank String name
) {}