
package com.example.demo.service.action;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.example.demo.Entity.ActionCondition;
import com.example.demo.dto.ActionInstruction;
import com.example.demo.enums.ComparisonOperator;
import com.example.demo.service.DiceService;

@Component
public class ActionInstructionValidator {

    private static final Set<String> TARGETS = Set.of("ACTOR", "TARGET");

    private static final Set<String> OPERATIONS = Set.of("ADD", "SUBTRACT");

    private static final Set<String> RESOURCES = Set.of("AP", "RP", "MP", "HP");

    private record ResultInfo(
            Class<?> type,
            ActionCondition condition) {
    }

    private final DiceService diceService;

    public ActionInstructionValidator(DiceService diceService) {
        this.diceService = diceService;
    }

    public void validate(List<ActionInstruction> instructions) {

        if (instructions == null || instructions.isEmpty()) {
            throw new IllegalArgumentException(
                    "Instructions cannot be empty");
        }

        Map<String, ResultInfo> results = new HashMap<>();

        for (ActionInstruction instruction : instructions) {

            if (instruction == null || instruction.type() == null) {
                throw new IllegalArgumentException(
                        "Instruction type is required");
            }

            validateCondition(instruction.when(), results);
            validateParams(instruction, results);
            registerResult(instruction, results);
        }
    }

    private void validateCondition(
            ActionCondition condition,
            Map<String, ResultInfo> results) {

        if (condition == null) {
            return;
        }

        String key = condition.resultKey();

        if (key == null || key.isBlank()
                || !results.containsKey(key)) {
            throw new IllegalArgumentException(
                    "Unknown condition result: " + key);
        }

        ComparisonOperator operator = condition.operator();

        if (operator == null) {
            throw new IllegalArgumentException(
                    "Condition operator is required");
        }

        Class<?> type = results.get(key).type();
        Object value = condition.value();

        if (type == Boolean.class) {

            if (!(value instanceof Boolean)) {
                throw new IllegalArgumentException(
                        "Boolean comparison value required");
            }

            if (operator != ComparisonOperator.EQUALS
                    && operator != ComparisonOperator.NOT_EQUALS) {
                throw new IllegalArgumentException(
                        "Invalid Boolean operator");
            }

        } else if (type == Integer.class) {

            requireInteger(value);

        } else {
            throw new IllegalArgumentException(
                    "Unsupported result type: " + type);
        }
    }

    private void validateParams(
            ActionInstruction instruction,
            Map<String, ResultInfo> results) {

        Map<String, Object> params = instruction.params();

        if (params == null) {
            throw new IllegalArgumentException(
                    "Instruction params are required");
        }

        switch (instruction.type()) {

            case ROLL_CHECK -> {
                requireString(params, "dice");
                requireInteger(params.get("difficulty"));
            }

            case ROLL_DAMAGE -> {
                requireString(params, "dice");
            }

            case CHANGE_HP -> {
                requireAllowed(params, "target", TARGETS);
                requireAllowed(params, "operation", OPERATIONS);
                requireIntegerResult(
                        params.get("amountFrom"),
                        instruction.when(),
                        results);
            }

            case CHANGE_RESOURCE -> {
                requireAllowed(params, "target", TARGETS);
                requireAllowed(params, "resource", RESOURCES);
                requireAllowed(params, "operation", OPERATIONS);
                requireIntegerResult(
                        params.get("amountFrom"),
                        instruction.when(),
                        results);
            }

            case APPLY_STATUS -> {
                requireAllowed(params, "target", TARGETS);
                requireString(params, "status");
            }
        }
    }

    private void registerResult(
            ActionInstruction instruction,
            Map<String, ResultInfo> results) {

        Class<?> resultType = switch (instruction.type()) {
            case ROLL_CHECK -> Boolean.class;
            case ROLL_DAMAGE -> Integer.class;
            default -> null;
        };

        String key = instruction.resultKey();

        if (resultType == null) {
            if (key != null) {
                throw new IllegalArgumentException(
                        "This instruction cannot produce a result");
            }
            return;
        }

        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException(
                    "Result key is required");
        }

        if (results.containsKey(key)) {
            throw new IllegalArgumentException(
                    "Duplicate result key: " + key);
        }

        results.put(
                key,
                new ResultInfo(resultType, instruction.when()));
    }

    private void requireIntegerResult(
            Object reference,
            ActionCondition consumerCondition,
            Map<String, ResultInfo> results) {

        if (!(reference instanceof String key)
                || key.isBlank()) {
            throw new IllegalArgumentException(
                    "amountFrom must be a result key");
        }

        ResultInfo info = results.get(key);

        if (info == null || info.type() != Integer.class) {
            throw new IllegalArgumentException(
                    "Expected an existing Integer result: " + key);
        }

        // 有条件产生的结果，不能被无条件读取。
        // 第一版只允许消费者使用相同条件。
        if (info.condition() != null
                && !info.condition().equals(consumerCondition)) {
            throw new IllegalArgumentException(
                    "Conditional result requires matching condition: " + key);
        }
    }

    private String requireString(
            Map<String, Object> params,
            String name) {

        Object value = params.get(name);

        if (!(value instanceof String text)
                || text.isBlank()) {
            throw new IllegalArgumentException(
                    "Invalid string parameter: " + name);
        }

        return text;
    }

    private void requireAllowed(
            Map<String, Object> params,
            String name,
            Set<String> allowed) {

        String value = requireString(params, name);

        if (!allowed.contains(value)) {
            throw new IllegalArgumentException(
                    "Invalid " + name + ": " + value);
        }
    }

    private int requireInteger(Object value) {

        if (value instanceof Integer number) {
            return number;
        }

        if (value instanceof Long number
                && number >= Integer.MIN_VALUE
                && number <= Integer.MAX_VALUE) {
            return number.intValue();
        }

        throw new IllegalArgumentException(
                "Expected an integer within int range");
    }
}
