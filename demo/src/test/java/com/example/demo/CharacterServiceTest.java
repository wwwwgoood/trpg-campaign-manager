package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.demo.DTO.CharacterResponse;
import com.example.demo.Entity.Campaign;
import com.example.demo.Entity.CampaignMember;
import com.example.demo.Entity.Character;
import com.example.demo.Entity.CharacterItem;
import com.example.demo.Entity.CharacterSkill;
import com.example.demo.Entity.NpcAffinity;
import com.example.demo.Repository.CampaignMemberRepository;
import com.example.demo.Repository.CharacterItemRepository;
import com.example.demo.Repository.CharacterRepository;
import com.example.demo.Repository.CharacterSkillRepository;
import com.example.demo.Repository.NpcAffinityRepository;
import com.example.demo.enums.CampaignMemberRole;
import com.example.demo.exception.BusinessRuleException;
import com.example.demo.mapper.CharacterMapper;
import com.example.demo.service.CharacterService;

@ExtendWith(MockitoExtension.class)
class CharacterServiceTest {

        @Mock
        private CharacterRepository characterRepository;

        @Mock
        private CampaignMemberRepository campaignMemberRepository;

        @Mock
        private CharacterSkillRepository characterSkillRepository;

        @Mock
        private CharacterItemRepository characterItemRepository;

        @Mock
        private NpcAffinityRepository npcAffinityRepository;

        @Mock
        private CharacterMapper characterMapper;

        @InjectMocks
        private CharacterService characterService;

        @Test
        void ownerShouldSeePrivateFields() {

                // Arrange
                Campaign campaign = new Campaign();
                campaign.setId(1L);

                CampaignMember owner = new CampaignMember();
                owner.setId(2L);
                owner.setCampaign(campaign);
                owner.setRole(CampaignMemberRole.PLAYER);

                Character character = new Character();
                character.setId(1L);
                character.setCampaign(campaign);
                character.setOwnerMember(owner);
                character.setCon(55);
                character.setLevel(1);

                List<CharacterSkill> skills = List.of();
                List<CharacterItem> items = List.of();
                List<NpcAffinity> affinities = List.of();

                CharacterResponse expectedResponse = new CharacterResponse();

                when(characterRepository.findById(1L))
                                .thenReturn(Optional.of(character));

                when(campaignMemberRepository.findById(2L))
                                .thenReturn(Optional.of(owner));

                when(characterSkillRepository.findByCharacter_Id(1L))
                                .thenReturn(skills);

                when(characterItemRepository.findByCharacter_Id(1L))
                                .thenReturn(items);

                when(npcAffinityRepository.findByNpcCharacter_Id(1L))
                                .thenReturn(affinities);

                when(characterMapper.toResponse(
                                eq(character),
                                eq(skills),
                                eq(items),
                                eq(affinities),
                                eq(115),
                                eq(true))).thenReturn(expectedResponse);

                // Act
                CharacterResponse actualResponse = characterService.getCharacterResponse(1L, 2L);

                // Assert
                assertSame(expectedResponse, actualResponse);
        }

        @Test
        void viewerFromAnotherCampaignShouldBeRejected() {

                // Arrange
                Campaign characterCampaign = new Campaign();
                characterCampaign.setId(1L);

                Campaign otherCampaign = new Campaign();
                otherCampaign.setId(2L);

                Character character = new Character();
                character.setId(1L);
                character.setCampaign(characterCampaign);

                CampaignMember outsider = new CampaignMember();
                outsider.setId(4L);
                outsider.setCampaign(otherCampaign);
                outsider.setRole(CampaignMemberRole.PLAYER);

                when(characterRepository.findById(1L))
                                .thenReturn(Optional.of(character));

                when(campaignMemberRepository.findById(4L))
                                .thenReturn(Optional.of(outsider));

                // Act + Assert
                assertThrows(
                                BusinessRuleException.class,
                                () -> characterService.getCharacterResponse(1L, 4L));
        }
}