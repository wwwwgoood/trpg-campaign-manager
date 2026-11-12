
package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.demo.Entity.Campaign;
import com.example.demo.Entity.CampaignAgent;
import com.example.demo.Entity.Character;
import com.example.demo.Entity.GameActionDefinition;
import com.example.demo.enums.ActionCostType;
import com.example.demo.enums.CampaignAgentType;
import com.example.demo.enums.CharacterType;
import com.example.demo.enums.GameActionKind;
import com.example.demo.repository.CampaignAgentRepository;
import com.example.demo.repository.CampaignRepository;
import com.example.demo.repository.CharacterRepository;
import com.example.demo.repository.GameActionDefinitionRepository;
import com.example.demo.service.action.ActionExecutor;
import com.example.demo.service.tool.AgentPrincipal;

@SpringBootTest
class ActionIntegrationTest {

    @Autowired CampaignRepository campaigns;
    @Autowired CampaignAgentRepository agents;
    @Autowired CharacterRepository characters;
    @Autowired GameActionDefinitionRepository actions;
    @Autowired ActionExecutor executor;
    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate transactions;

    private static final String NORMAL = """
        [
          {
            "type": "ROLL_CHECK",
            "resultKey": "check",
            "params": {
              "dice": "1d20",
              "difficulty": 10
            }
          }
        ]
        """;

    private static final String FAILING = """
        [
          {
            "type": "ROLL_DAMAGE",
            "resultKey": "damage",
            "params": {"dice": "1d6"}
          },
          {
            "type": "CHANGE_HP",
            "params": {
              "target": "ACTOR",
              "amountFrom": "damage",
              "operation": "SUBTRACT"
            }
          },
          {
            "type": "APPLY_STATUS",
            "params": {
              "target": "ACTOR",
              "status": "TEST_STATUS"
            }
          }
        ]
        """;

    @Test
    void actionIntegrationTest() throws Exception {

        Long[] ids = new Long[5];

        ExecutorService pool =
                Executors.newFixedThreadPool(2);

        try {
            // 1. 准备并提交测试数据
            transactions.executeWithoutResult(status -> {

                Campaign campaign = campaigns.save(
                        new Campaign("Action集成测试", null, null));
                ids[0] = campaign.getId();

                CampaignAgent agent = new CampaignAgent();
                agent.setName("测试AI");
                agent.setCampaign(campaign);
                agent.setAgentType(CampaignAgentType.AI_PLAYER);
                agent.setEnabled(true);
                agent = agents.save(agent);
                ids[1] = agent.getId();

                GameActionDefinition normal = createAction(
                        "正常行动", NORMAL);
                ids[2] = normal.getId();

                GameActionDefinition failing = createAction(
                        "失败行动", FAILING);
                ids[3] = failing.getId();

                Character actor = new Character();
                actor.setName("集成测试角色");
                actor.setCampaign(campaign);
                actor.setOwnerAgent(agent);
                actor.setCharacterType(
                        CharacterType.PLAYER_CHARACTER);
                actor.setCurrentAp(2);
                actor.setCurrentHp(10);
                actor.setKnownActions(
                        List.of(normal, failing));

                actor = characters.save(actor);
                ids[4] = actor.getId();
            });

            AgentPrincipal principal =
                    new AgentPrincipal(ids[0], ids[1]);

            // 2. 正常执行
            executor.executeAsAgent(
                    principal, ids[2], ids[4], null);

            assertEquals(1, getAp(ids[4]));

            // 恢复初始值
            setAp(ids[4], 2);

            // 3. 异常回滚
            assertThrows(
                    UnsupportedOperationException.class,
                    () -> executor.executeAsAgent(
                            principal, ids[3], ids[4], null));

            assertEquals(2, getAp(ids[4]));
            assertEquals(10, getHp(ids[4]));

            // 4. 两线程争抢最后1 AP
            setAp(ids[4], 1);

            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch start = new CountDownLatch(1);

            Callable<Boolean> task = () -> {
                ready.countDown();
                start.await();

                try {
                    executor.executeAsAgent(
                            principal, ids[2], ids[4], null);
                    return true;
                } catch (IllegalArgumentException e) {
                    if (!"Insufficient resource"
                            .equals(e.getMessage())) {
                        throw e;
                    }
                    return false;
                }
            };

            Future<Boolean> a = pool.submit(task);
            Future<Boolean> b = pool.submit(task);

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            boolean resultA = a.get(15, TimeUnit.SECONDS);
            boolean resultB = b.get(15, TimeUnit.SECONDS);

            // 恰好一个成功
            assertNotEquals(resultA, resultB);

            // 最终只扣除1 AP
            assertEquals(0, getAp(ids[4]));

        } finally {
            pool.shutdownNow();

            // 按外键依赖顺序清理测试数据
            transactions.executeWithoutResult(status -> {
                if (ids[4] != null) {
                    jdbc.update(
                        "DELETE FROM character_game_action " +
                        "WHERE character_id = ?", ids[4]);
                    jdbc.update(
                        "DELETE FROM character WHERE id = ?",
                        ids[4]);
                }
                if (ids[1] != null) {
                    jdbc.update(
                        "DELETE FROM campaign_agent WHERE id = ?",
                        ids[1]);
                }
                if (ids[2] != null) {
                    jdbc.update(
                        "DELETE FROM game_action_definition WHERE id = ?",
                        ids[2]);
                }
                if (ids[3] != null) {
                    jdbc.update(
                        "DELETE FROM game_action_definition WHERE id = ?",
                        ids[3]);
                }
                if (ids[0] != null) {
                    jdbc.update(
                        "DELETE FROM campaign WHERE id = ?",
                        ids[0]);
                }
            });
        }
    }

    private GameActionDefinition createAction(
            String name, String instructions) {

        GameActionDefinition action =
                new GameActionDefinition();

        action.setName(name);
        action.setKind(GameActionKind.ATTACK);
        action.setCostType(ActionCostType.AP);
        action.setCostValue(1);
        action.setInstructions(instructions);

        return actions.save(action);
    }

    private int getAp(Long id) {
        return jdbc.queryForObject(
                "SELECT current_ap FROM character WHERE id = ?",
                Integer.class, id);
    }

    private int getHp(Long id) {
        return jdbc.queryForObject(
                "SELECT current_hp FROM character WHERE id = ?",
                Integer.class, id);
    }

    private void setAp(Long id, int ap) {
        jdbc.update(
                "UPDATE character SET current_ap = ? WHERE id = ?",
                ap, id);
    }
}
