


package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

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

@SpringBootTest
class ActionTransactionIntegrationTest {

    @Autowired CampaignRepository campaigns;
    @Autowired CampaignAgentRepository agents;
    @Autowired CharacterRepository characters;
    @Autowired GameActionDefinitionRepository actions;
    @Autowired ActionExecutor executor;
    @Autowired JdbcTemplate jdbc;
    @Autowired TransactionTemplate transactions;

    @Test
    void failedActionShouldRollbackDatabaseChanges() {

        // 测试数据的ID
        Long[] ids = new Long[4];

        try {
            // 先提交测试数据，建立独立的初始状态
            transactions.executeWithoutResult(status -> {

                Campaign campaign = campaigns.save(
                        new Campaign("事务回滚测试", null, null));
                ids[0] = campaign.getId();

                CampaignAgent agent = new CampaignAgent();
                agent.setCampaign(campaign);
                agent.setName("测试AI");
                agent.setAgentType(CampaignAgentType.AI_PLAYER);
                agent = agents.save(agent);
                ids[1] = agent.getId();

                GameActionDefinition action =
                        new GameActionDefinition();
                action.setName("事务失败测试行动");
                action.setKind(GameActionKind.ATTACK);
                action.setCostType(ActionCostType.AP);
                action.setCostValue(1);

                action.setInstructions("""
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
                    """);

                action = actions.save(action);
                ids[2] = action.getId();

                Character actor = new Character();
                actor.setCampaign(campaign);
                actor.setOwnerAgent(agent);
                actor.setCharacterType(
                        CharacterType.PLAYER_CHARACTER);
                actor.setName("事务测试角色");
                actor.setCurrentHp(10);
                actor.setCurrentAp(2);
                actor.setKnownActions(List.of(action));

                actor = characters.save(actor);
                ids[3] = actor.getId();
            });

            // 以已归属的AI身份执行行动
            var principal =
                    new com.example.demo.service.tool.AgentPrincipal(
                            ids[0], ids[1]);

            // Action预期在APPLY_STATUS时失败
            assertThrows(
                    UnsupportedOperationException.class,
                    () -> executor.executeAsAgent(
                            principal, ids[2], ids[3], null));

            // 用SQL重新查询，不使用原先的Java对象
            Integer ap = jdbc.queryForObject(
                    "SELECT current_ap FROM character WHERE id = ?",
                    Integer.class, ids[3]);

            Integer hp = jdbc.queryForObject(
                    "SELECT current_hp FROM character WHERE id = ?",
                    Integer.class, ids[3]);

            assertEquals(2, ap);
            assertEquals(10, hp);

        } finally {
            // 清理本次创建的测试数据
            transactions.executeWithoutResult(status -> {
                if (ids[3] != null) {
                    jdbc.update(
                            "DELETE FROM character_game_action WHERE character_id = ?",
                            ids[3]);
                    jdbc.update(
                            "DELETE FROM character WHERE id = ?",
                            ids[3]);
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
                if (ids[0] != null) {
                    jdbc.update(
                            "DELETE FROM campaign WHERE id = ?",
                            ids[0]);
                }
            });
        }
    }
}
