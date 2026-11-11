package com.example.demo.service.action;

import com.example.demo.Entity.GameActionDefinition;

public interface ActionRuntime {

    // 检查登录用户是否能控制 actor，
    // 角色与目标是否属于同一战役，
    // actor 是否掌握行动，以及目标是否合法。
    void requireCanExecute(
            Long actorId,
            Long targetId,
            GameActionDefinition action);

    // 检查并扣除 AP、RP 等行动消耗
    void consumeActionCost(
            Long actorId,
            GameActionDefinition action);

    // 修改 HP（delta 可正可负）
    void changeHp(Long characterId, int delta);

    // 修改资源
    void changeResource(
            Long characterId,
            String resource,
            int delta);

    // 施加状态
    void applyStatus(Long characterId, String status);
}