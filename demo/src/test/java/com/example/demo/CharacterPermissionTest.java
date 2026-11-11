package com.example.demo;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.Entity.Campaign;
import com.example.demo.Entity.CampaignMember;
import com.example.demo.Entity.Character;
import com.example.demo.Entity.User;
import com.example.demo.enums.CampaignMemberRole;
import com.example.demo.enums.CharacterType;
import com.example.demo.enums.SkillType;
import com.example.demo.repository.CampaignMemberRepository;
import com.example.demo.repository.CharacterItemRepository;
import com.example.demo.repository.CharacterRepository;
import com.example.demo.repository.CharacterSkillRepository;
import com.example.demo.repository.NpcAffinityRepository;
import com.example.demo.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
class CharacterPermissionTest {

    private Campaign campaign;
    private Character character;

    private User dmUser;
    private User playerA;
    private User playerB;
    private User outsider;

    private CampaignMember dmMember;
    private CampaignMember memberA;
    private CampaignMember memberB;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CharacterSkillRepository characterSkillRepository;

    @MockitoBean
    private NpcAffinityRepository npcAffinityRepository;

    @MockitoBean
    private CharacterItemRepository characterItemRepository;

    @MockitoBean
    private CharacterRepository characterRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private CampaignMemberRepository memberRepository;

    @Test
    void unauthenticatedUserCannotReadCharacter() throws Exception {

        mockMvc.perform(
                get("/api/characters/1"))
                .andExpect(status().isUnauthorized());
    }

    @BeforeEach
    void setUp() {

        // ========== 1. 创建战役 ==========

        campaign = new Campaign();
        campaign.setId(1L);
        campaign.setName("1892芝加哥战役");

        // ========== 2. 创建四个用户 ==========

        dmUser = new User();
        dmUser.setId(2L);
        dmUser.setUsername("dm_test");

        playerA = new User();
        playerA.setId(3L);
        playerA.setUsername("player_a_test");

        playerB = new User();
        playerB.setId(4L);
        playerB.setUsername("player_b_test");

        outsider = new User();
        outsider.setId(5L);
        outsider.setUsername("other_player_test");

        // ========== 3. 创建战役成员 ==========

        // DM
        dmMember = new CampaignMember();
        dmMember.setId(1L);
        dmMember.setUser(dmUser);
        dmMember.setCampaign(campaign);
        dmMember.setDisplayName("DM");
        dmMember.setRole(CampaignMemberRole.DM);

        // 玩家 A
        memberA = new CampaignMember();
        memberA.setId(2L);
        memberA.setUser(playerA);
        memberA.setCampaign(campaign);
        memberA.setDisplayName("PLAYER_A");
        memberA.setRole(CampaignMemberRole.PLAYER);

        // 玩家 B
        memberB = new CampaignMember();
        memberB.setId(3L);
        memberB.setUser(playerB);
        memberB.setCampaign(campaign);
        memberB.setDisplayName("PLAYER_B");
        memberB.setRole(CampaignMemberRole.PLAYER);

        // outsider 不创建战役 1 的成员关系
        // 因为他不属于战役 1

        // ========== 4. 创建角色 ==========

        character = new Character();
        character.setId(10L);
        character.setCampaign(campaign);

        // 角色属于玩家 A
        character.setOwnerMember(memberA);

        character.setCharacterType(CharacterType.PLAYER_CHARACTER);
        character.setName("Lilith");

        // 基础属性
        character.setStr(10);
        character.setDex(10);
        character.setCon(10);
        character.setIntel(10);
        character.setApp(10);
        character.setEdu(10);

        character.setLevel(1);
        character.setCurrentHp(12);

        // 私人字段
        character.setSecret("隐藏的真实身份");
        character.setDesire("获得强大的超凡力量");
        character.setFear("失去自由");

        // ========== 5. 模拟角色查询 ==========

        when(characterRepository.findById(10L)).thenReturn(Optional.of(character));

        // ========== 6. 模拟用户查询 ==========

        when(userRepository.findByUsername("dm_test"))
                .thenReturn(Optional.of(dmUser));

        when(userRepository.findByUsername("player_a_test"))
                .thenReturn(Optional.of(playerA));

        when(userRepository.findByUsername("player_b_test"))
                .thenReturn(Optional.of(playerB));

        when(userRepository.findByUsername("other_player_test"))
                .thenReturn(Optional.of(outsider));

        // ========== 7. 模拟成员查询 ==========

        when(memberRepository.findByUser_IdAndCampaign_Id(2L, 1L))
                .thenReturn(Optional.of(dmMember));

        when(memberRepository.findByUser_IdAndCampaign_Id(3L, 1L))
                .thenReturn(Optional.of(memberA));

        when(memberRepository.findByUser_IdAndCampaign_Id(4L, 1L))
                .thenReturn(Optional.of(memberB));

        // 非战役成员
        when(memberRepository.findByUser_IdAndCampaign_Id(5L, 1L))
                .thenReturn(Optional.empty());

        when(characterRepository.save(any(Character.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void nonMemberCannotReadCharacter() throws Exception {

        // 1. 准备战役
        Campaign campaign = new Campaign();
        campaign.setId(1L);

        // 2. 准备角色
        Character character = new Character();
        character.setId(10L);
        character.setCampaign(campaign);
        character.setName("Lilith");

        // 3. 准备登录用户
        User outsider = new User();
        outsider.setId(5L);
        outsider.setUsername("other_player_test");
        User playerB = new User();

        playerB.setId(4L);
        playerB.setUsername("player_b_test");

        CampaignMember memberB = new CampaignMember();
        memberB.setId(3L);
        memberB.setUser(playerB);
        memberB.setCampaign(campaign);
        memberB.setRole(CampaignMemberRole.PLAYER);

        when(memberRepository.findByUser_IdAndCampaign_Id(4L, 1L))
                .thenReturn(Optional.of(memberB));

        when(userRepository.findByUsername("player_b_test"))
                .thenReturn(Optional.of(playerB));

        // 4. 模拟数据库查询
        when(characterRepository.findById(10L))
                .thenReturn(Optional.of(character));

        when(userRepository.findByUsername("other_player_test"))
                .thenReturn(Optional.of(outsider));

        // 此用户不是战役 1 的成员
        when(memberRepository.findByUser_IdAndCampaign_Id(5L, 1L))
                .thenReturn(Optional.empty());

        // 5. 发起请求并验证结果
        mockMvc.perform(
                get("/api/characters/10")
                        .with(user("other_player_test")))
                .andExpect(status().isForbidden());

        mockMvc.perform(
                get("/api/characters/10")
                        .with(user("player_b_test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Lilith"))

                // Secret
                .andExpect(jsonPath("$.secret.value").value(nullValue()))
                .andExpect(jsonPath("$.secret.visible").value(false))

                // Desire
                .andExpect(jsonPath("$.desire.value").value(nullValue()))
                .andExpect(jsonPath("$.desire.visible").value(false))

                // Fear
                .andExpect(jsonPath("$.fear.value").value(nullValue()))
                .andExpect(jsonPath("$.fear.visible").value(false));
    }

    @Test
    void ownerCanViewPrivateFields() throws Exception {

        mockMvc.perform(
                get("/api/characters/10")
                        .with(user("player_a_test")))
                .andExpect(status().isOk())

                .andExpect(jsonPath("$.secret.value")
                        .value("隐藏的真实身份"))
                .andExpect(jsonPath("$.secret.visible")
                        .value(true))

                .andExpect(jsonPath("$.desire.value")
                        .value("获得强大的超凡力量"))
                .andExpect(jsonPath("$.desire.visible")
                        .value(true))

                .andExpect(jsonPath("$.fear.value")
                        .value("失去自由"))
                .andExpect(jsonPath("$.fear.visible")
                        .value(true));
    }

    @Test
    void dmCanViewPrivateFields() throws Exception {

        mockMvc.perform(
                get("/api/characters/10")
                        .with(user("dm_test")))
                .andExpect(status().isOk())

                .andExpect(jsonPath("$.secret.value")
                        .value("隐藏的真实身份"))
                .andExpect(jsonPath("$.secret.visible")
                        .value(true))

                .andExpect(jsonPath("$.desire.value")
                        .value("获得强大的超凡力量"))
                .andExpect(jsonPath("$.desire.visible")
                        .value(true))

                .andExpect(jsonPath("$.fear.value")
                        .value("失去自由"))
                .andExpect(jsonPath("$.fear.visible")
                        .value(true));
    }

    @Test
    void otherMemberCannotDeleteCharacter() throws Exception {

        mockMvc.perform(
                delete("/api/characters/10")
                        .with(user("player_b_test"))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        // 确认角色没有被删除
        verify(characterRepository, never())
                .delete(any(Character.class));
    }

    @Test
    void otherMemberCannotUpdateCharacter() throws Exception {

        mockMvc.perform(
                put("/api/characters/10")
                        .with(user("player_b_test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Hacked Name"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void dmCanDeleteCharacter() throws Exception {

        mockMvc.perform(
                delete("/api/characters/10")
                        .with(user("dm_test"))
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(characterRepository).delete(character);
    }

    @Test
    void ownerCanUpdateCharacter() throws Exception {

        mockMvc.perform(
                put("/api/characters/10")
                        .with(user("player_a_test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Lilith Updated"
                                }
                                """))
                .andExpect(status().isOk());

        // 验证角色名称已经改变
        assertEquals("Lilith Updated", character.getName());
    }

    @Test
    void playerCannotCreateNpc() throws Exception {

        mockMvc.perform(
                post("/api/campaigns/1/npcs")
                        .with(user("player_a_test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Unauthorized NPC"
                                }
                                """))
                .andExpect(status().isForbidden());

        // 确认没有保存角色
        verify(characterRepository, never())
                .save(any(Character.class));

    }

    @Test
    void playerCannotAssignCharacterToAnotherMember() throws Exception {

        // 基于之前准备的合法玩家角色 JSON
        String json = validPlayerCharacterJson()
                .replace(
                        "\"name\": \"New Adventurer\"",
                        "\"name\": \"New Adventurer\", \"ownerMemberId\": 3");

        mockMvc.perform(
                post("/api/campaigns/1/player-characters")
                        .with(user("player_a_test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        // 捕获实际保存的角色
        ArgumentCaptor<Character> captor = ArgumentCaptor.forClass(Character.class);

        verify(characterRepository).save(captor.capture());

        Character savedCharacter = captor.getValue();

        // 验证角色仍然属于玩家 A
        assertEquals(
                2L,
                savedCharacter.getOwnerMember().getId());
    }

    @Test
    void dmCanCreateCharacterForPlayer() throws Exception {

        // 模拟 DM 查询指定的角色拥有者
        when(memberRepository.findById(3L))
                .thenReturn(Optional.of(memberB));

        // 构造合法角色数据，指定玩家 B 为拥有者
        String json = validPlayerCharacterJson()
                .replace(
                        "\"name\": \"New Adventurer\"",
                        "\"name\": \"New Adventurer\", \"ownerMemberId\": 3");

        // DM 发起创建请求
        mockMvc.perform(
                post("/api/campaigns/1/player-characters")
                        .with(user("dm_test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        // 捕获实际保存的角色
        ArgumentCaptor<Character> captor = ArgumentCaptor.forClass(Character.class);

        verify(characterRepository).save(captor.capture());

        Character savedCharacter = captor.getValue();

        // 验证角色归属于玩家 B
        assertEquals(
                memberB.getId(),
                savedCharacter.getOwnerMember().getId());
    }

    @Test
    void dmCanCreateNpc() throws Exception {

        mockMvc.perform(
                post("/api/campaigns/1/npcs")
                        .with(user("dm_test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Oscar",
                                    "gender": "Male",
                                    "occupation": "Scholar",
                                    "level": 1,
                                    "rank": 6,
                                    "str": 10,
                                    "dex": 10,
                                    "con": 10,
                                    "intel": 14,
                                    "app": 12,
                                    "edu": 15,
                                    "currentHp": 10,
                                    "skills": [],
                                    "items": [],
                                    "featIds": [],
                                    "affinities": []
                                }
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<Character> captor = ArgumentCaptor.forClass(Character.class);

        verify(characterRepository).save(captor.capture());

        Character createdNpc = captor.getValue();

        assertEquals(CharacterType.NPC, createdNpc.getCharacterType());

        assertNull(createdNpc.getOwnerMember());

        assertEquals("Oscar", createdNpc.getName());
    }

    private String validPlayerCharacterJson() {

        return """
                {
                    "name": "New Adventurer",
                    "gender": "Female",
                    "nationality": "American",
                    "occupation": "Scholar",
                    "identity": "Traveler",

                    "level": 1,
                    "rank": 6,

                    "str": 10,
                    "dex": 12,
                    "con": 10,
                    "intel": 15,
                    "app": 14,
                    "edu": 13,

                    "currentHp": 12,

                    "cashCents": 10000,
                    "debtCents": 0,
                    "incomeCents": 0,
                    "expenseCents": 0,

                    "desire": "Explore the unknown",
                    "fear": "Losing freedom",
                    "secret": "Hidden identity",

                    "skills": [
                        {
                            "skillType": "%s",
                            "value": 50,
                            "unknown": false
                        }
                    ],

                    "featIds": [],
                    "items": [],
                    "affinities": []
                }
                """.formatted(SkillType.values()[0].name());
    }
}