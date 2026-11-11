package com.example.demo.dto;

import java.util.List;

public record DiceRollResult(
        String expression,
        List<Integer> rolls,
        int modifier,
        int total
) {}