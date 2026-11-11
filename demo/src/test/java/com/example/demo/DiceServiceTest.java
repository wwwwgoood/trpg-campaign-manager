package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import com.example.demo.dto.DiceRollResult;
import com.example.demo.service.DiceService;

class DiceServiceTest {

    private final DiceService diceService =
            new DiceService(new Random(42));

    @Test
    void rollShouldReturnCorrectStructure() {

        DiceRollResult result = diceService.roll("2d6+3");

        assertEquals(2, result.rolls().size());
        assertEquals(3, result.modifier());

        assertTrue(result.rolls().stream()
                .allMatch(value -> value >= 1 && value <= 6));

        assertEquals(
                result.rolls().stream()
                    .mapToInt(Integer::intValue)
                    .sum() + 3,
                result.total()
        );
    }

    @Test
    void invalidExpressionShouldBeRejected() {

        assertThrows(IllegalArgumentException.class,
                () -> diceService.roll("abc"));

        assertThrows(IllegalArgumentException.class,
                () -> diceService.roll("10000d6"));

        assertThrows(IllegalArgumentException.class,
                () -> diceService.roll("2d0"));

        assertThrows(IllegalArgumentException.class,
                () -> diceService.roll("999999999999999999d6"));
    }
}