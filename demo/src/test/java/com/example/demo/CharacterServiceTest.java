package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.Entity.Campaign;
import com.example.demo.Entity.CampaignMember;
import com.example.demo.Entity.Character;
import com.example.demo.Entity.CharacterItem;
import com.example.demo.Entity.CharacterSkill;
import com.example.demo.Entity.NpcAffinity;
import com.example.demo.Entity.User;
import com.example.demo.dto.CharacterResponse;
import com.example.demo.enums.CampaignMemberRole;
import com.example.demo.mapper.CharacterMapper;
import com.example.demo.repository.CampaignMemberRepository;
import com.example.demo.repository.CampaignRepository;
import com.example.demo.repository.CharacterItemRepository;
import com.example.demo.repository.CharacterRepository;
import com.example.demo.repository.CharacterSkillRepository;
import com.example.demo.repository.NpcAffinityRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.CampaignAccessService;
import com.example.demo.service.CharacterService;

@SpringBootTest
@AutoConfigureMockMvc
class CharacterServiceTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private CharacterRepository characterRepository;

        @MockitoBean
        private CampaignMemberRepository campaignMemberRepository;

        @MockitoBean
        private CampaignRepository campaignRepository;

        @MockitoBean
        private CharacterSkillRepository characterSkillRepository;

        @MockitoBean
        private CharacterItemRepository characterItemRepository;

        @MockitoBean
        private NpcAffinityRepository npcAffinityRepository;

        @MockitoBean
        private CharacterMapper characterMapper;

        @MockitoBean
        private CampaignAccessService campaignAccessService;

        @Autowired
        private CharacterService characterService;

        private Campaign campaign;
        private CampaignMember memberA;
        private CampaignMember memberB;
        private CampaignMember dmMember;

        @MockitoBean
        private UserRepository userRepository;

        @BeforeEach
        void setup() {

                campaign = new Campaign();
                campaign.setId(100L);

                User dmUser = new User();
                dmUser.setId(2L);
                dmUser.setUsername("dm_test");

                User playerA = new User();
                playerA.setId(3L);
                playerA.setUsername("player_a_test");

                User playerB = new User();
                playerB.setId(4L);
                playerB.setUsername("player_b_test");

                dmMember = new CampaignMember();
                dmMember.setId(1L);
                dmMember.setCampaign(campaign);
                dmMember.setUser(dmUser);
                dmMember.setRole(CampaignMemberRole.DM);

                memberA = new CampaignMember();
                memberA.setId(2L);
                memberA.setCampaign(campaign);
                memberA.setUser(playerA);
                memberA.setRole(CampaignMemberRole.PLAYER);

                memberB = new CampaignMember();
                memberB.setId(3L);
                memberB.setCampaign(campaign);
                memberB.setUser(playerB);
                memberB.setRole(CampaignMemberRole.PLAYER);

                Character character = new Character();
                character.setCampaign(campaign);
                character.setName("test");
                character.setId(1L);

                when(userRepository.findByUsername("dm_test"))
                                .thenReturn(Optional.of(dmUser));

                when(userRepository.findByUsername("player_a_test"))
                                .thenReturn(Optional.of(playerA));

                when(userRepository.findByUsername("player_b_test"))
                                .thenReturn(Optional.of(playerB));

                when(characterRepository.findById(1L))
                                .thenReturn(Optional.of(character));

                when(campaignRepository.findById(100L)).thenReturn(Optional.of(campaign));
        }

        @Test
        void ownerShouldSeePrivateFields() {

                Campaign ownerCampaign = new Campaign();
                ownerCampaign.setId(1L);

                CampaignMember owner = new CampaignMember();
                owner.setId(2L);
                owner.setCampaign(ownerCampaign);
                owner.setRole(CampaignMemberRole.PLAYER);

                Character character = new Character();
                character.setId(1L);
                character.setCampaign(ownerCampaign);
                character.setOwnerMember(owner);
                character.setCon(55);
                character.setLevel(1);

                List<CharacterSkill> skills = List.of();
                List<CharacterItem> items = List.of();
                List<NpcAffinity> affinities = List.of();

                CharacterResponse expectedResponse = new CharacterResponse();

                when(characterRepository.findById(1L))
                                .thenReturn(Optional.of(character));

                // 模拟认证后的当前战役成员
                when(campaignAccessService.requireMember(1L))
                                .thenReturn(owner);

                when(characterSkillRepository.findByCharacter_Id(1L))
                                .thenReturn(skills);

                when(characterItemRepository.findByCharacter_Id(1L))
                                .thenReturn(items);

                when(npcAffinityRepository.findByNpcCharacter_Id(1L))
                                .thenReturn(affinities);

                // 只针对角色和权限标记进行匹配
                when(characterMapper.toResponse(
                                eq(character),
                                eq(skills),
                                eq(items),
                                eq(affinities),
                                anyInt(),
                                eq(true))).thenReturn(expectedResponse);

                CharacterResponse actualResponse = characterService.getCharacterResponse(1L);

                assertSame(expectedResponse, actualResponse);
        }

        @Test
        void viewerFromAnotherCampaignShouldBeRejected() throws Exception {

                when(campaignAccessService.requireMember(100L))
                                .thenThrow(new ResponseStatusException(
                                                HttpStatus.FORBIDDEN,
                                                "You are not a member of this campaign"));

                mockMvc.perform(
                                get("/api/characters/1")
                                                .with(user("player_b_test")))
                                .andExpect(status().isForbidden());

                verify(characterRepository).findById(1L);
                verify(campaignAccessService).requireMember(100L);
        }
}