
package com.example.demo.service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.dto.DiceRollResult;

@Service
public class DiceService {

    private static final Pattern DICE_PATTERN =
            Pattern.compile(
                    "([1-9]\\d*)?[dD]([1-9]\\d*)([+-]\\d+)?"
            );

    private static final int MAX_DICE = 100;
    private static final int MAX_SIDES = 1000;
    private static final int MAX_MODIFIER = 10000;

    private final RandomGenerator random;

    @Autowired
    public DiceService() {
        this(new SecureRandom());
    }

    // 允许测试注入固定随机数生成器
    public DiceService(RandomGenerator random) {
        this.random = java.util.Objects.requireNonNull(random);
    }

    private record DiceSpec(
            int count,
            int sides,
            int modifier
    ) {}

    public void validateExpression(String expression) {
        parse(expression);
    }

    public DiceRollResult roll(String expression) {

        DiceSpec spec = parse(expression);

        List<Integer> rolls = new ArrayList<>();

        int total = spec.modifier();

        for (int i = 0; i < spec.count(); i++) {

            int value = random.nextInt(1, spec.sides() + 1);

            rolls.add(value);
            total += value;
        }

        return new DiceRollResult(
                expression,
                List.copyOf(rolls),
                spec.modifier(),
                total
        );
    }

    private DiceSpec parse(String expression) {

        if (expression == null) {
            throw new IllegalArgumentException(
                    "Dice expression is required");
        }

        Matcher matcher =
                DICE_PATTERN.matcher(expression.trim());

        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "Invalid dice expression: " + expression);
        }

        try {
            int count = matcher.group(1) == null
                    ? 1
                    : Integer.parseInt(matcher.group(1));

            int sides = Integer.parseInt(matcher.group(2));

            int modifier = matcher.group(3) == null
                    ? 0
                    : Integer.parseInt(matcher.group(3));

            if (count < 1 || count > MAX_DICE) {
                throw new IllegalArgumentException(
                        "Dice count out of range");
            }

            if (sides < 2 || sides > MAX_SIDES) {
                throw new IllegalArgumentException(
                        "Dice sides out of range");
            }

            if (modifier < -MAX_MODIFIER
                    || modifier > MAX_MODIFIER) {
                throw new IllegalArgumentException(
                        "Dice modifier out of range");
            }

            return new DiceSpec(count, sides, modifier);

        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Dice number out of range", exception);
        }
    }
}
