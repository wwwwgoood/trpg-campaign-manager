package com.example.demo.service.tool;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.dto.DiceRollResult;
import com.example.demo.service.DiceService;
import com.example.demo.service.action.ActionExecutor;

@Service
public class GameToolService {

    private final DiceService diceService;
    private final ActionExecutor actionExecutor;

    public GameToolService(
            DiceService diceService,
            ActionExecutor actionExecutor) {

        this.diceService = diceService;
        this.actionExecutor = actionExecutor;
    }

    // 工具一：掷骰

    public DiceRollResult rollDice(
            ToolContext context,
            String expression) {

        requireContext(context);

        return diceService.roll(expression);
    }

    private void requireContext(ToolContext context) {

        if (context == null
                || context.campaignId() == null
                || context.userId() == null
                || context.callerType() == null) {

            throw new IllegalArgumentException(
                    "Invalid tool context");
        }
    }

    // 工具二：执行游戏行动
    public List<String> executeAction(
            Long actionId,
            Long actorId,
            Long targetId) {

        return actionExecutor.execute(
                actionId, actorId, targetId);
    }

    public DiceRollResult rollDiceStateless(String expression) {
        return diceService.roll(expression);
    }

    public List<String> executeActionAsAgent(
            AgentPrincipal principal,
            Long actionId,
            Long actorId,
            Long targetId) {

        return actionExecutor.executeAsAgent(
                principal, actionId, actorId, targetId);
    }
}