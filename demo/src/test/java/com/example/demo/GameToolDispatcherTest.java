
package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.demo.dto.DiceRollResult;
import com.example.demo.service.tool.GameToolDispatcher;
import com.example.demo.service.tool.GameToolService;
import com.example.demo.service.tool.ToolCallerType;
import com.example.demo.service.tool.ToolContext;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class GameToolDispatcherTest {

    private GameToolService gameToolService;
    private GameToolDispatcher dispatcher;
    private ObjectMapper mapper;
    private ToolContext context;

    @BeforeEach
    void setUp() {
        gameToolService = mock(GameToolService.class);
        dispatcher = new GameToolDispatcher(gameToolService);
        mapper = new ObjectMapper();

        context = new ToolContext(
                1L, 3L, ToolCallerType.HUMAN);
    }

    @Test
    void shouldDispatchRollDice() throws Exception {
        JsonNode arguments = mapper.readTree("""
            {
                "expression": "2d6+3"
            }
            """);

        DiceRollResult expected = new DiceRollResult(
                "2d6+3", List.of(2, 5), 3, 10);

        when(gameToolService.rollDice(context, "2d6+3"))
                .thenReturn(expected);

        Object result = dispatcher.dispatch(
                context, "roll_dice", arguments);

        assertSame(expected, result);

        verify(gameToolService)
                .rollDice(context, "2d6+3");
    }

    @Test
    void shouldRejectUnknownTool() throws Exception {
        JsonNode arguments = mapper.readTree("{}");

        assertThrows(
                IllegalArgumentException.class,
                () -> dispatcher.dispatch(
                        context, "delete_campaign", arguments));

        verifyNoInteractions(gameToolService);
    }

    @Test
    void shouldRejectInvalidArguments() throws Exception {
        JsonNode arguments = mapper.readTree("""
            {
                "actionId": "abc",
                "actorId": 1
            }
            """);

        assertThrows(
                IllegalArgumentException.class,
                () -> dispatcher.dispatch(
                        context, "execute_action", arguments));

        verifyNoInteractions(gameToolService);
    }

    @Test
    void shouldRejectUnimplementedAiAuthorization()
            throws Exception {

        ToolContext aiContext = new ToolContext(
                1L, 3L, ToolCallerType.AI_DM);

        JsonNode arguments = mapper.readTree("""
            {
                "expression": "1d20"
            }
            """);

        assertThrows(
                UnsupportedOperationException.class,
                () -> dispatcher.dispatch(
                        aiContext, "roll_dice", arguments));

        verifyNoInteractions(gameToolService);
    }
}
