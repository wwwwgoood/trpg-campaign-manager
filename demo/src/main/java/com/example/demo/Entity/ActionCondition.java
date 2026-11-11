package com.example.demo.Entity;

import com.example.demo.enums.ComparisonOperator;

public record ActionCondition(
    String resultKey,
    ComparisonOperator operator,
    Object value
) {}
