
package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.example.demo.Entity.CampaignMember;
import com.example.demo.enums.CampaignMemberRole;
import com.example.demo.service.CampaignAccessService;
import com.example.demo.service.ai.AiAgentService;
import com.example.demo.service.tool.AgentPrincipal;

@SpringBootTest
class AiAgentEndToEndTest {

    private static final Long CAMPAIGN_ID = 2L;
    private static final Long ACTION_ID = 4L;

    @Autowired
    private AiAgentService aiAgentService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 本测试只模拟真人 DM 登录授权。
    // AI 的角色归属、战役隔离和行动执行仍使用真实数据库。
    @MockitoBean
    private CampaignAccessService accessService;

    @Test
    @EnabledIfEnvironmentVariable(
            named = "DEEPSEEK_E2E_TEST",
            matches = "true"
    )
    void shouldExecuteActionThroughRealDeepSeek() {

        Long agentId = jdbcTemplate.queryForObject("""
                SELECT id
                FROM campaign_agent
                WHERE campaign_id = ?
                  AND name = 'E2E测试AI玩家'
                  AND enabled = TRUE
                ORDER BY id DESC
                LIMIT 1
                """, Long.class, CAMPAIGN_ID);

        assertNotNull(agentId);

        Long actorId = jdbcTemplate.queryForObject("""
                SELECT id
                FROM character
                WHERE campaign_id = ?
                  AND owner_agent_id = ?
                  AND name = 'AI测试战士'
                ORDER BY id DESC
                LIMIT 1
                """, Long.class, CAMPAIGN_ID, agentId);

        assertNotNull(actorId);

        Integer learned = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM character_game_action
                WHERE character_id = ?
                  AND action_id = ?
                """, Integer.class, actorId, ACTION_ID);

        assertEquals(1, learned);

        // 模拟当前请求来自战役 DM
        CampaignMember dm = mock(CampaignMember.class);

        when(dm.getRole()).thenReturn(CampaignMemberRole.DM);

        when(accessService.requireMember(CAMPAIGN_ID))
                .thenReturn(dm);

        // 确保初始 AP 固定为 2
        jdbcTemplate.update("""
                UPDATE character
                SET current_ap = 2
                WHERE id = ?
                """, actorId);

        AgentPrincipal principal =
                new AgentPrincipal(CAMPAIGN_ID, agentId);

        String answer = null;

        try {
            // 真正调用 DeepSeek API
            answer = aiAgentService.chatAsAgent(
                    principal,
                    """
                    你正在进行 TRPG 游戏工具测试。

                    请实际调用 execute_action 工具恰好一次。
                    参数必须是：
                    actionId = %d
                    actorId = %d

                    不需要 targetId。

                    必须等待工具执行结果，
                    然后如实说明行动是否成功。
                    不要再次调用 execute_action。
                    """.formatted(ACTION_ID, actorId)
            );

            System.out.println("DeepSeek 最终回复：");
            System.out.println(answer);

            Integer apAfter = getAp(actorId);

            System.out.println("行动前 AP：2");
            System.out.println("行动后 AP：" + apAfter);

            assertNotNull(answer);
            assertFalse(answer.isBlank());

            // 真正的数据库断言
            assertEquals(1, apAfter,
                    "AI 应当恰好执行一次消耗 1 AP 的行动");

        } finally {
            // 无论通过还是失败，都恢复测试角色的 AP
            jdbcTemplate.update("""
                    UPDATE character
                    SET current_ap = 2
                    WHERE id = ?
                    """, actorId);
        }
    }

    private Integer getAp(Long actorId) {
        return jdbcTemplate.queryForObject("""
                SELECT current_ap
                FROM character
                WHERE id = ?
                """, Integer.class, actorId);
    }
}
