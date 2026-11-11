package com.example.demo.dto;

import java.util.Map;

import com.example.demo.Entity.ActionCondition;
import com.example.demo.enums.ActionInstructionType;

public record ActionInstruction(
        ActionInstructionType type,
        ActionCondition when,
        String resultKey,
        Map<String, Object> params
) {}