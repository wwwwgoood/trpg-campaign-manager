
package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.Entity.Character;
import com.example.demo.Entity.GameActionDefinition;
import com.example.demo.dto.ActionToolResponse;
import com.example.demo.enums.ActionCostType;
import com.example.demo.mapper.CharacterMapper;
import com.example.demo.repository.CampaignMemberRepository;
import com.example.demo.repository.CampaignRepository;
import com.example.demo.repository.CharacterItemRepository;
import com.example.demo.repository.CharacterRepository;
import com.example.demo.repository.CharacterSkillRepository;
import com.example.demo.repository.FeatRepository;
import com.example.demo.repository.NpcAffinityRepository;
import com.example.demo.repository.SkillRepository;
import com.example.demo.service.CampaignAccessService;
import com.example.demo.service.CharacterService;
import com.example.demo.service.character.AgentCharacterAccessService;
import com.example.demo.service.tool.AgentPrincipal;

@ExtendWith(MockitoExtension.class)
class CharacterAvailableActionsTest {

    @Mock CampaignRepository campaignRepository;
    @Mock CampaignMemberRepository campaignMemberRepository;
    @Mock CharacterRepository characterRepository;
    @Mock SkillRepository skillRepository;
    @Mock CharacterItemRepository characterItemRepository;
    @Mock CharacterSkillRepository characterSkillRepository;
    @Mock FeatRepository featRepository;
    @Mock NpcAffinityRepository npcAffinityRepository;
    @Mock CharacterMapper characterMapper;
    @Mock CampaignAccessService campaignAccessService;
    @Mock AgentCharacterAccessService agentCharacterAccessService;

    @InjectMocks
    CharacterService characterService;

    @Test
    void shouldReturnLearnedActionsWithCorrectAffordability() {

        AgentPrincipal principal = new AgentPrincipal(1L, 10L);

        Character character = new Character();
        character.setId(17L);
        character.setCurrentAp(2);
        character.setCurrentRp(1);

        GameActionDefinition attack = action(1L, "普通攻击",
                ActionCostType.AP, 1);

        GameActionDefinition combo = action(2L, "连续攻击",
                ActionCostType.AP, 3);

        GameActionDefinition block = action(3L, "格挡",
                ActionCostType.RP, 1);

        GameActionDefinition counter = action(4L, "反击",
                ActionCostType.RP, 2);

        GameActionDefinition hpAction = action(5L, "生命消耗",
                ActionCostType.HP, 1);

        character.setKnownActions(List.of(
                attack, combo, block, counter, hpAction));

        when(characterRepository.findById(17L))
                .thenReturn(Optional.of(character));

        List<ActionToolResponse> result =
                characterService.getAvailableActionsForAgent(
                        principal, 17L);

        assertEquals(5, result.size());

        assertTrue(result.get(0).affordable());
        assertFalse(result.get(1).affordable());
        assertTrue(result.get(2).affordable());
        assertFalse(result.get(3).affordable());
        assertFalse(result.get(4).affordable());

        assertEquals(1L, result.get(0).id());
        assertEquals("普通攻击", result.get(0).name());
        assertEquals(ActionCostType.AP, result.get(0).costType());
        assertEquals(1, result.get(0).costValue());

        verify(agentCharacterAccessService)
                .requireControl(principal, character);
    }

    private GameActionDefinition action(
            Long id,
            String name,
            ActionCostType costType,
            int costValue) {

        GameActionDefinition action = new GameActionDefinition();
        action.setId(id);
        action.setName(name);
        action.setCostType(costType);
        action.setCostValue(costValue);

        return action;
    }
}
