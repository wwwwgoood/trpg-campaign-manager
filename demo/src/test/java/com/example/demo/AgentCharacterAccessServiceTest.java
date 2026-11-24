
package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.Entity.Campaign;
import com.example.demo.Entity.CampaignAgent;
import com.example.demo.Entity.CampaignMember;
import com.example.demo.Entity.Character;
import com.example.demo.enums.CampaignAgentType;
import com.example.demo.repository.CampaignAgentRepository;
import com.example.demo.service.character.AgentCharacterAccessService;
import com.example.demo.service.tool.AgentPrincipal;

@ExtendWith(MockitoExtension.class)
class AgentCharacterAccessServiceTest {

    @Mock
    private CampaignAgentRepository agentRepository;

    @InjectMocks
    private AgentCharacterAccessService accessService;

    private Campaign campaign;
    private CampaignAgent agent;
    private Character character;
    private AgentPrincipal principal;

    @BeforeEach
    void setUp() {
        campaign = new Campaign();
        campaign.setId(1L);

        agent = new CampaignAgent();
        agent.setId(10L);
        agent.setCampaign(campaign);
        agent.setEnabled(true);
        agent.setAgentType(CampaignAgentType.AI_PLAYER);

        character = new Character();
        character.setId(17L);
        character.setCampaign(campaign);

        principal = new AgentPrincipal(1L, 10L);
    }

    @Test
    void aiPlayerCanControlOwnCharacter() {
        character.setOwnerAgent(agent);

        when(agentRepository.findById(10L))
                .thenReturn(Optional.of(agent));

        assertDoesNotThrow(() ->
                accessService.requireControl(principal, character));
    }

    @Test
    void aiPlayerCannotControlAnotherCharacter() {
        CampaignAgent anotherAgent = new CampaignAgent();
        anotherAgent.setId(20L);

        character.setOwnerAgent(anotherAgent);

        when(agentRepository.findById(10L))
                .thenReturn(Optional.of(agent));

        assertThrows(ResponseStatusException.class, () ->
                accessService.requireControl(principal, character));
    }

    @Test
    void aiDmCanControlUnownedNpc() {
        agent.setAgentType(CampaignAgentType.AI_DM);

        when(agentRepository.findById(10L))
                .thenReturn(Optional.of(agent));

        assertDoesNotThrow(() ->
                accessService.requireControl(principal, character));
    }

    @Test
    void aiDmCannotControlHumanCharacter() {
        agent.setAgentType(CampaignAgentType.AI_DM);

        CampaignMember member = new CampaignMember();
        character.setOwnerMember(member);

        when(agentRepository.findById(10L))
                .thenReturn(Optional.of(agent));

        assertThrows(ResponseStatusException.class, () ->
                accessService.requireControl(principal, character));
    }

    @Test
    void cannotControlCharacterFromAnotherCampaign() {
        Campaign otherCampaign = new Campaign();
        otherCampaign.setId(2L);

        character.setCampaign(otherCampaign);

        // 跨战役应在查询 Agent 之前就被拒绝
        assertThrows(ResponseStatusException.class, () ->
                accessService.requireControl(principal, character));

        verifyNoInteractions(agentRepository);
    }

    @Test
    void disabledAgentCannotControlCharacter() {
        agent.setEnabled(false);
        character.setOwnerAgent(agent);

        when(agentRepository.findById(10L))
                .thenReturn(Optional.of(agent));

        assertThrows(ResponseStatusException.class, () ->
                accessService.requireControl(principal, character));
    }
}
