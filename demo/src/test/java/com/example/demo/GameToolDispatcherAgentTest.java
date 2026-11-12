
package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.Entity.Campaign;
import com.example.demo.Entity.CampaignAgent;
import com.example.demo.Entity.CampaignMember;
import com.example.demo.enums.CampaignMemberRole;
import com.example.demo.repository.CampaignAgentRepository;
import com.example.demo.service.CampaignAccessService;
import com.example.demo.service.tool.AgentPrincipal;
import com.example.demo.service.tool.GameToolDispatcher;
import com.example.demo.service.tool.GameToolService;

class GameToolDispatcherAgentTest {

    private CampaignAgentRepository agentRepository;
    private CampaignAccessService accessService;
    private GameToolDispatcher dispatcher;

    @BeforeEach
    void setUp() {

        agentRepository = mock(CampaignAgentRepository.class);
        accessService = mock(CampaignAccessService.class);

        dispatcher = new GameToolDispatcher(
                mock(GameToolService.class),
                accessService,
                agentRepository
        );
    }

    @Test
    void shouldAllowDmToLaunchEnabledAgent() {

        CampaignMember member = mock(CampaignMember.class);
        when(member.getRole()).thenReturn(CampaignMemberRole.DM);

        when(accessService.requireMember(1L))
                .thenReturn(member);

        Campaign campaign = mock(Campaign.class);
        when(campaign.getId()).thenReturn(1L);

        CampaignAgent agent = mock(CampaignAgent.class);
        when(agent.getCampaign()).thenReturn(campaign);
        when(agent.isEnabled()).thenReturn(true);

        when(agentRepository.findById(20L))
                .thenReturn(Optional.of(agent));

        assertDoesNotThrow(() ->
                dispatcher.requireAgentLaunch(
                        new AgentPrincipal(1L, 20L)));
    }

    @Test
    void shouldRejectAgentFromAnotherCampaign() {

        CampaignMember member = mock(CampaignMember.class);
        when(member.getRole()).thenReturn(CampaignMemberRole.DM);

        when(accessService.requireMember(1L))
                .thenReturn(member);

        Campaign otherCampaign = mock(Campaign.class);
        when(otherCampaign.getId()).thenReturn(2L);

        CampaignAgent agent = mock(CampaignAgent.class);
        when(agent.getCampaign()).thenReturn(otherCampaign);

        when(agentRepository.findById(20L))
                .thenReturn(Optional.of(agent));

        assertThrows(ResponseStatusException.class, () ->
                dispatcher.requireAgentLaunch(
                        new AgentPrincipal(1L, 20L)));
    }

    @Test
    void shouldRejectNonDm() {

        CampaignMember member = mock(CampaignMember.class);
        when(member.getRole())
                .thenReturn(CampaignMemberRole.PLAYER);

        when(accessService.requireMember(1L))
                .thenReturn(member);

        assertThrows(ResponseStatusException.class, () ->
                dispatcher.requireAgentLaunch(
                        new AgentPrincipal(1L, 20L)));

        verifyNoInteractions(agentRepository);
    }
}
