
package com.example.demo.service.action;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.Entity.GameActionDefinition;
import com.example.demo.dto.ActionInstruction;
import com.example.demo.dto.DiceRollResult;
import com.example.demo.repository.GameActionDefinitionRepository;
import com.example.demo.service.DiceService;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
public class ActionExecutor {

    private final GameActionDefinitionRepository actionRepository;
    private final ActionInstructionValidator validator;
    private final ActionConditionEvaluator evaluator;
    private final DiceService diceService;
    private final ActionRuntime runtime;
    private final ObjectMapper objectMapper;

    public ActionExecutor(
            GameActionDefinitionRepository actionRepository,
            ActionInstructionValidator validator,
            ActionConditionEvaluator evaluator,
            DiceService diceService,
            ActionRuntime runtime,
            ObjectMapper objectMapper) {

        this.actionRepository = actionRepository;
        this.validator = validator;
        this.evaluator = evaluator;
        this.diceService = diceService;
        this.runtime = runtime;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public List<String> execute(
            Long actionId,
            Long actorId,
            Long targetId) {

        // 1. 获取行动规则
        GameActionDefinition action = actionRepository.findById(actionId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Action not found"));

        // 2. 检查身份、战役、行动掌握情况、目标是否合法
        runtime.requireCanExecute(actorId, targetId, action);

        // 3. 解析并校验完整指令列表
        List<ActionInstruction> instructions =
                parseInstructions(action.getInstructions());

        validator.validate(instructions);

        // 4. 检查并扣除行动资源
        runtime.consumeActionCost(actorId, action);

        // 5. 创建本次执行上下文
        ActionExecutionContext context =
                new ActionExecutionContext(actorId, targetId);

        List<String> events = new ArrayList<>();

        // 6. 逐条执行指令
        for (ActionInstruction instruction : instructions) {

            // 条件不成立，跳过
            if (!evaluator.evaluate(instruction.when(), context)) {
                continue;
            }

            Map<String, Object> params = instruction.params();

            switch (instruction.type()) {

                case ROLL_CHECK -> {
                    DiceRollResult roll = diceService.roll(
                            (String) params.get("dice"));

                    int difficulty =
                            ((Number) params.get("difficulty")).intValue();

                    boolean success = roll.total() >= difficulty;

                    context.putResult(instruction.resultKey(), success);

                    events.add("检定: " + roll.total()
                            + " / " + difficulty
                            + "，成功: " + success);
                }

                case ROLL_DAMAGE -> {
                    DiceRollResult roll = diceService.roll(
                            (String) params.get("dice"));

                    context.putResult(
                            instruction.resultKey(), roll.total());

                    events.add("掷骰结果: " + roll.total());
                }

                case CHANGE_HP -> {
                    Long target = resolveTarget(
                            (String) params.get("target"), context);

                    int amount = requireAmount(params, context);

                    String operation = (String) params.get("operation");

                    int delta = operation.equals("SUBTRACT")
                            ? -amount : amount;

                    runtime.changeHp(target, delta);

                    events.add("角色 " + target
                            + " HP变化: " + delta);
                }

                case CHANGE_RESOURCE -> {
                    Long target = resolveTarget(
                            (String) params.get("target"), context);

                    int amount = requireAmount(params, context);

                    String operation = (String) params.get("operation");

                    int delta = operation.equals("SUBTRACT")
                            ? -amount : amount;

                    runtime.changeResource(
                            target,
                            (String) params.get("resource"),
                            delta);

                    events.add("角色 " + target
                            + " 资源变化: " + delta);
                }

                case APPLY_STATUS -> {
                    Long target = resolveTarget(
                            (String) params.get("target"), context);

                    String status = (String) params.get("status");

                    runtime.applyStatus(target, status);

                    events.add("角色 " + target
                            + " 获得状态: " + status);
                }
            }
        }

        return List.copyOf(events);
    }

    private List<ActionInstruction> parseInstructions(String json) {
        try {
            return objectMapper.readValue(
                    json,
                    new TypeReference<List<ActionInstruction>>() {});
        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Invalid action instructions", exception);
        }
    }

    private int requireAmount(
            Map<String, Object> params,
            ActionExecutionContext context) {

        String key = (String) params.get("amountFrom");

        // 不存在、类型不正确，都会明确抛出异常
        int amount = context.getInteger(key);

        if (amount < 0) {
            throw new IllegalArgumentException(
                    "Amount cannot be negative");
        }

        return amount;
    }

    private Long resolveTarget(
            String target,
            ActionExecutionContext context) {

        return switch (target) {
            case "ACTOR" -> context.getActorId();
            case "TARGET" -> {
                if (context.getTargetId() == null) {
                    throw new IllegalArgumentException(
                            "Target is required");
                }
                yield context.getTargetId();
            }
            default -> throw new IllegalArgumentException(
                    "Invalid target: " + target);
        };
    }
}
