
package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.Entity.Campaign;
import com.example.demo.Entity.CampaignAgent;
import com.example.demo.Entity.CampaignMember;
import com.example.demo.enums.CampaignAgentType;
import com.example.demo.enums.CampaignMemberRole;
import com.example.demo.repository.CampaignAgentRepository;
import com.example.demo.repository.CampaignRepository;
import com.example.demo.service.CampaignAccessService;
import com.example.demo.service.CampaignAgentService;

class CampaignAgentServiceTest {

    private CampaignAgentRepository agentRepository;
    private CampaignRepository campaignRepository;
    private CampaignAccessService accessService;
    private CampaignAgentService service;

    @BeforeEach
    void setUp() {
        agentRepository = mock(CampaignAgentRepository.class);
        campaignRepository = mock(CampaignRepository.class);
        accessService = mock(CampaignAccessService.class);

        service = new CampaignAgentService(
                agentRepository,
                campaignRepository,
                accessService
        );
    }

    @Test
    void dmCanCreateAiPlayer() {

        CampaignMember dm = new CampaignMember();
        dm.setRole(CampaignMemberRole.DM);

        Campaign campaign = new Campaign();
        campaign.setId(1L);

        when(accessService.requireMember(1L))
                .thenReturn(dm);

        when(campaignRepository.findById(1L))
                .thenReturn(Optional.of(campaign));

        when(agentRepository.save(any(CampaignAgent.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        CampaignAgent result = service.createAgent(
                1L,
                "艾琳",
                CampaignAgentType.AI_PLAYER
        );

        assertEquals("艾琳", result.getName());
        assertEquals(CampaignAgentType.AI_PLAYER,
                result.getAgentType());

        assertEquals(1L, result.getCampaign().getId());
        assertTrue(result.isEnabled());

        verify(agentRepository).save(any(CampaignAgent.class));
    }

    @Test
    void playerCannotCreateAiDm() {

        CampaignMember player = new CampaignMember();
        player.setRole(CampaignMemberRole.PLAYER);

        when(accessService.requireMember(1L))
                .thenReturn(player);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createAgent(
                        1L,
                        "AI主持人",
                        CampaignAgentType.AI_DM
                )
        );

        assertEquals(HttpStatus.FORBIDDEN,
                exception.getStatusCode());

        verifyNoInteractions(
                campaignRepository,
                agentRepository);
    }

    @Test
    void dmCannotDisableAgentFromAnotherCampaign() {

        CampaignMember dm = new CampaignMember();
        dm.setRole(CampaignMemberRole.DM);

        Campaign otherCampaign = new Campaign();
        otherCampaign.setId(2L);

        CampaignAgent otherAgent = new CampaignAgent();
        otherAgent.setId(10L);
        otherAgent.setCampaign(otherCampaign);
        otherAgent.setEnabled(true);

        when(accessService.requireMember(1L))
                .thenReturn(dm);

        when(agentRepository.findById(10L))
                .thenReturn(Optional.of(otherAgent));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.setEnabled(1L, 10L, false)
        );

        assertEquals(HttpStatus.NOT_FOUND,
                exception.getStatusCode());

        // 确认操作没有改变 Agent 状态
        assertTrue(otherAgent.isEnabled());

        verify(agentRepository, never())
                .save(any(CampaignAgent.class));
    }
}
