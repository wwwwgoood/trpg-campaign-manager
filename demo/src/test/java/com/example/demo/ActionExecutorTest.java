
package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.example.demo.Entity.GameActionDefinition;
import com.example.demo.dto.DiceRollResult;
import com.example.demo.enums.ActionCostType;
import com.example.demo.repository.GameActionDefinitionRepository;
import com.example.demo.service.DiceService;
import com.example.demo.service.action.ActionConditionEvaluator;
import com.example.demo.service.action.ActionExecutor;
import com.example.demo.service.action.ActionInstructionValidator;
import com.example.demo.service.action.ActionRuntime;

import tools.jackson.databind.ObjectMapper;

class ActionExecutorTest {

    @Test
    void attackShouldConsumeApAndDealDamage() {

        GameActionDefinitionRepository actionRepository =
                mock(GameActionDefinitionRepository.class);

        ActionRuntime runtime = mock(ActionRuntime.class);
        DiceService diceService = mock(DiceService.class);

        ActionInstructionValidator validator =
                mock(ActionInstructionValidator.class);

        ActionConditionEvaluator evaluator =
                new ActionConditionEvaluator();

        ActionExecutor executor = new ActionExecutor(
                actionRepository,
                validator,
                evaluator,
                diceService,
                runtime,
                new ObjectMapper()
        );

        // 1. 构造行动定义
        GameActionDefinition action =
                new GameActionDefinition();

        action.setId(10L);
        action.setCostType(ActionCostType.AP);
        action.setCostValue(1);

        action.setInstructions("""
            [
                {
                    "type": "ROLL_DAMAGE",
                    "resultKey": "damage",
                    "params": {
                        "dice": "1d8+3"
                    }
                },
                {
                    "type": "CHANGE_HP",
                    "params": {
                        "target": "TARGET",
                        "amountFrom": "damage",
                        "operation": "SUBTRACT"
                    }
                }
            ]
            """);

        when(actionRepository.findById(10L))
                .thenReturn(Optional.of(action));

        // 2. 模拟骰子结果：伤害为 7
        when(diceService.roll("1d8+3"))
                .thenReturn(new DiceRollResult(
                        "1d8+3",
                        List.of(4),
                        3,
                        7
                ));

        // 3. 执行行动
        List<String> events =
                executor.execute(10L, 1L, 2L);

        // 4. 验证执行器调用了权限检查
        verify(runtime).requireCanExecute(
                1L, 2L, action);

        // 5. 验证消耗行动资源
        verify(runtime).consumeActionCost(1L, action);

        // 6. 验证目标受到 7 点伤害
        verify(runtime).changeHp(2L, -7);

        // 7. 验证产生了执行日志
        assertFalse(events.isEmpty());
    }

    @Test
void missedAttackShouldNotDealDamage() {

    GameActionDefinitionRepository actionRepository =
            mock(GameActionDefinitionRepository.class);

    ActionRuntime runtime = mock(ActionRuntime.class);
    DiceService diceService = mock(DiceService.class);

    ActionInstructionValidator validator =
            mock(ActionInstructionValidator.class);

    ActionConditionEvaluator evaluator =
            new ActionConditionEvaluator();

    ActionExecutor executor = new ActionExecutor(
            actionRepository,
            validator,
            evaluator,
            diceService,
            runtime,
            new ObjectMapper()
    );

    GameActionDefinition action = new GameActionDefinition();
    action.setId(10L);
    action.setCostType(ActionCostType.AP);
    action.setCostValue(1);

    action.setInstructions("""
        [
          {
            "type": "ROLL_CHECK",
            "resultKey": "hit",
            "params": {
              "dice": "d20",
              "difficulty": 15
            }
          },
          {
            "type": "ROLL_DAMAGE",
            "when": {
              "resultKey": "hit",
              "operator": "EQUALS",
              "value": true
            },
            "resultKey": "damage",
            "params": {
              "dice": "1d8+3"
            }
          },
          {
            "type": "CHANGE_HP",
            "when": {
              "resultKey": "hit",
              "operator": "EQUALS",
              "value": true
            },
            "params": {
              "target": "TARGET",
              "amountFrom": "damage",
              "operation": "SUBTRACT"
            }
          }
        ]
        """);

    when(actionRepository.findById(10L))
            .thenReturn(Optional.of(action));

    // 固定检定结果为 5，低于难度 15
    when(diceService.roll("d20"))
            .thenReturn(new DiceRollResult(
                    "d20",
                    List.of(5),
                    0,
                    5
            ));

    List<String> events = executor.execute(10L, 1L, 2L);

    // 行动消耗仍然发生
    verify(runtime).consumeActionCost(1L, action);

    // 没有执行伤害掷骰
    verify(diceService, never()).roll("1d8+3");

    // 目标没有受到伤害
    verify(runtime, never()).changeHp(anyLong(), anyInt());

    assertEquals(1, events.size());
}
}
