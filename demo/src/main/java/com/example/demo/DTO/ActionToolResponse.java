package com.example.demo.dto;

import com.example.demo.enums.ActionCostType;

public record ActionToolResponse(
        Long id,
        String name,
        String description,
        String range,
        ActionCostType costType,
        Integer costValue,
        boolean affordable
) {}