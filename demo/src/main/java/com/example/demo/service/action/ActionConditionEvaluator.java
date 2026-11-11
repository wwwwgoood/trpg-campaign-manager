package com.example.demo.service.action;

import org.springframework.stereotype.Component;

import com.example.demo.Entity.ActionCondition;
import com.example.demo.enums.ComparisonOperator;

@Component
public class ActionConditionEvaluator {

    public boolean evaluate(
            ActionCondition condition,
            ActionExecutionContext context) {

        if (condition == null) {
            return true;
        }

        Object actual = context.getResult(condition.resultKey());
        Object expected = condition.value();
        ComparisonOperator operator = condition.operator();

        if (operator == null) {
            throw new IllegalArgumentException("Operator is required");
        }

        if (actual instanceof Boolean a) {
            if (!(expected instanceof Boolean b)) {
                throw new IllegalArgumentException("Expected Boolean");
            }

            return switch (operator) {
                case EQUALS -> a.equals(b);
                case NOT_EQUALS -> !a.equals(b);
                default -> throw new IllegalArgumentException(
                        "Unsupported Boolean operator");
            };
        }

        if (actual instanceof Integer a) {
            int b;

            if (expected instanceof Integer n) {
                b = n;
            } else if (expected instanceof Long n
                    && n >= Integer.MIN_VALUE
                    && n <= Integer.MAX_VALUE) {
                b = n.intValue();
            } else {
                throw new IllegalArgumentException("Expected Integer");
            }

            return switch (operator) {
                case EQUALS -> a == b;
                case NOT_EQUALS -> a != b;
                case GREATER_THAN -> a > b;
                case GREATER_OR_EQUAL -> a >= b;
                case LESS_THAN -> a < b;
                case LESS_OR_EQUAL -> a <= b;
            };
        }

        throw new IllegalArgumentException(
                "Unsupported result type");
    }
}