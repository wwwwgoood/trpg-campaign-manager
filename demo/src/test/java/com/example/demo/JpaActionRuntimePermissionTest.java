
package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.Entity.Campaign;
import com.example.demo.Entity.CampaignAgent;
import com.example.demo.Entity.CampaignMember;
import com.example.demo.Entity.Character;
import com.example.demo.Entity.GameActionDefinition;
import com.example.demo.enums.CampaignAgentType;
import com.example.demo.repository.CampaignAgentRepository;
import com.example.demo.repository.CharacterRepository;
import com.example.demo.service.CampaignAccessService;
import com.example.demo.service.action.JpaActionRuntime;
import com.example.demo.service.tool.AgentPrincipal;

class JpaActionRuntimePermissionTest {

    // 每组参数：
    // 场景名称、角色归属、是否跨战役、
    // Agent是否启用、是否掌握行动、预期是否允许
    static Stream<Arguments> permissionCases() {
        return Stream.of(
                Arguments.of(
                        "自己的角色", "SELF",
                        false, true, true, true),

                Arguments.of(
                        "其他AI的角色", "OTHER_AI",
                        false, true, true, false),

                Arguments.of(
                        "真人角色", "HUMAN",
                        false, true, true, false),

                Arguments.of(
                        "跨Campaign", "SELF",
                        true, true, true, false),

                Arguments.of(
                        "禁用Agent", "SELF",
                        false, false, true, false),

                Arguments.of(
                        "未掌握Action", "SELF",
                        false, true, false, false));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("permissionCases")
    void testAiPlayerPermission(
            String scenario,
            String ownership,
            boolean crossCampaign,
            boolean enabled,
            boolean knowsAction,
            boolean expectedAllowed) {

        // 1. 模拟依赖
        var characters = mock(CharacterRepository.class);
        var agents = mock(CampaignAgentRepository.class);
        var access = mock(CampaignAccessService.class);

        var runtime = new JpaActionRuntime(
                characters, access, agents);

        // 2. 构造战役
        Campaign campaign = new Campaign();
        campaign.setId(1L);

        Campaign otherCampaign = new Campaign();
        otherCampaign.setId(2L);

        // 3. 构造调用者
        CampaignAgent caller = new CampaignAgent();
        caller.setId(10L);
        caller.setCampaign(campaign);
        caller.setAgentType(CampaignAgentType.AI_PLAYER);
        caller.setEnabled(enabled);

        CampaignAgent otherAgent = new CampaignAgent();
        otherAgent.setId(20L);
        otherAgent.setCampaign(campaign);

        CampaignMember human = new CampaignMember();
        human.setId(30L);

        // 4. 构造角色
        Character actor = new Character();
        actor.setId(100L);
        actor.setCurrentAp(2);

        actor.setCampaign(
                crossCampaign ? otherCampaign : campaign);

        switch (ownership) {
            case "SELF" -> actor.setOwnerAgent(caller);
            case "OTHER_AI" -> actor.setOwnerAgent(otherAgent);
            case "HUMAN" -> actor.setOwnerMember(human);
            default -> throw new IllegalArgumentException(
                    "Unknown ownership");
        }

        // 5. 构造行动
        GameActionDefinition action = new GameActionDefinition();
        action.setId(5L);

        actor.setKnownActions(
                knowsAction ? List.of(action) : List.of());

        // 6. 配置模拟仓库
        when(characters.findByIdForUpdate(100L))
                .thenReturn(Optional.of(actor));

        when(agents.findById(10L))
                .thenReturn(Optional.of(caller));

        AgentPrincipal principal = new AgentPrincipal(1L, 10L);

        // 7. 验证预期结果
        if (expectedAllowed) {
            assertDoesNotThrow(() -> runtime.requireCanExecute(
                    principal, 100L, null, action));
        } else {
            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> runtime.requireCanExecute(
                            principal, 100L, null, action));

            assertEquals(
                    HttpStatus.FORBIDDEN,
                    exception.getStatusCode());
        }

        // 8. 仅执行权限检查，AP不应被消耗
        assertEquals(2, actor.getCurrentAp());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("aiDmCases")
    void testAiDmPermission(
            String scenario,
            String ownership,
            boolean expectedAllowed) {

        var characters = mock(CharacterRepository.class);
        var agents = mock(CampaignAgentRepository.class);
        var access = mock(CampaignAccessService.class);

        var runtime = new JpaActionRuntime(
                characters, access, agents);

        Campaign campaign = new Campaign();
        campaign.setId(1L);

        CampaignAgent dm = new CampaignAgent();
        dm.setId(10L);
        dm.setCampaign(campaign);
        dm.setAgentType(CampaignAgentType.AI_DM);
        dm.setEnabled(true);

        CampaignAgent otherAgent = new CampaignAgent();
        otherAgent.setId(20L);

        CampaignMember human = new CampaignMember();
        human.setId(30L);

        Character actor = new Character();
        actor.setId(100L);
        actor.setCampaign(campaign);
        actor.setCurrentAp(2);

        switch (ownership) {
            case "AI" -> actor.setOwnerAgent(otherAgent);
            case "HUMAN" -> actor.setOwnerMember(human);
            case "NONE" -> {
                // NPC无归属者
            }
            default -> throw new IllegalArgumentException();
        }

        GameActionDefinition action = new GameActionDefinition();
        action.setId(5L);
        actor.setKnownActions(List.of(action));

        when(characters.findByIdForUpdate(100L))
                .thenReturn(Optional.of(actor));

        when(agents.findById(10L))
                .thenReturn(Optional.of(dm));

        AgentPrincipal principal = new AgentPrincipal(1L, 10L);

        if (expectedAllowed) {
            assertDoesNotThrow(() -> runtime.requireCanExecute(
                    principal, 100L, null, action));
        } else {
            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> runtime.requireCanExecute(
                            principal, 100L, null, action));

            assertEquals(HttpStatus.FORBIDDEN,
                    exception.getStatusCode());
        }

        assertEquals(2, actor.getCurrentAp());
    }

    static Stream<Arguments> aiDmCases() {
        return Stream.of(
                Arguments.of("AI DM控制NPC", "NONE", true),
                Arguments.of("AI DM控制其他AI角色", "AI", false),
                Arguments.of("AI DM控制真人角色", "HUMAN", false));
    }


@Test
void aiCannotTargetAnotherCampaign() {

    var characters = mock(CharacterRepository.class);
    var agents = mock(CampaignAgentRepository.class);
    var access = mock(CampaignAccessService.class);

    var runtime = new JpaActionRuntime(
            characters, access, agents);

    Campaign campaign1 = new Campaign();
    campaign1.setId(1L);

    Campaign campaign2 = new Campaign();
    campaign2.setId(2L);

    CampaignAgent agent = new CampaignAgent();
    agent.setId(10L);
    agent.setCampaign(campaign1);
    agent.setAgentType(CampaignAgentType.AI_PLAYER);
    agent.setEnabled(true);

    Character actor = new Character();
    actor.setId(100L);
    actor.setCampaign(campaign1);
    actor.setOwnerAgent(agent);
    actor.setCurrentAp(2);

    Character target = new Character();
    target.setId(200L);
    target.setCampaign(campaign2);
    target.setCurrentHp(10);

    GameActionDefinition action =
            new GameActionDefinition();
    action.setId(5L);

    actor.setKnownActions(List.of(action));

    when(characters.findByIdForUpdate(100L))
            .thenReturn(Optional.of(actor));

    when(characters.findById(200L))
            .thenReturn(Optional.of(target));

    when(agents.findById(10L))
            .thenReturn(Optional.of(agent));

    AgentPrincipal principal =
            new AgentPrincipal(1L, 10L);

    ResponseStatusException exception =
            assertThrows(
                    ResponseStatusException.class,
                    () -> runtime.requireCanExecute(
                            principal, 100L, 200L, action));

    assertEquals(HttpStatus.FORBIDDEN,
            exception.getStatusCode());

    assertEquals(2, actor.getCurrentAp());
    assertEquals(10, target.getCurrentHp());
}


}
