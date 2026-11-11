package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.Entity.Campaign;
import com.example.demo.Entity.CampaignMember;
import com.example.demo.Entity.Memo;
import com.example.demo.Entity.User;
import com.example.demo.enums.CampaignMemberRole;
import com.example.demo.exception.CampaignNotFoundException;
import com.example.demo.repository.CampaignMemberRepository;
import com.example.demo.repository.CampaignRepository;
import com.example.demo.repository.MemoRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.MemoService;

@SpringBootTest
@AutoConfigureMockMvc
public class MemoServiceTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private MemoRepository memoRepository;

        @MockitoBean
        private CampaignMemberRepository memberRepository;

        @MockitoBean
        private UserRepository userRepository;

        @Autowired
        private MemoService memoService;

        @MockitoBean
        private CampaignRepository campaignRepository;

        private Campaign campaign;
        private CampaignMember memberA;
        private CampaignMember memberB;
        private CampaignMember dmMember;

        private Memo privateMemo;
        private Memo publicMemo;

        @BeforeEach
        void setUp() {

                // 1. 创建测试战役
                campaign = new Campaign();
                campaign.setId(1L);

                // 2. 创建测试用户
                User dmUser = new User();
                dmUser.setId(2L);
                dmUser.setUsername("dm_test");

                User playerA = new User();
                playerA.setId(3L);
                playerA.setUsername("player_a_test");

                User playerB = new User();
                playerB.setId(4L);
                playerB.setUsername("player_b_test");

                // 3. 创建战役成员
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

                // 4. 创建玩家 B 的私人备忘录
                privateMemo = new Memo(
                                campaign,
                                "Private Memo",
                                null,
                                "Player B secret");
                privateMemo.setId(20L);
                privateMemo.setOwnerMember(memberB);
                privateMemo.setPrivate(true);

                // 5. 创建玩家 B 的公共备忘录
                publicMemo = new Memo(
                                campaign,
                                "Public Memo",
                                null,
                                "Shared information");
                publicMemo.setId(21L);
                publicMemo.setOwnerMember(memberB);
                publicMemo.setPrivate(false);

                // 6. 模拟用户查询
                when(userRepository.findByUsername("dm_test"))
                                .thenReturn(Optional.of(dmUser));

                when(userRepository.findByUsername("player_a_test"))
                                .thenReturn(Optional.of(playerA));

                when(userRepository.findByUsername("player_b_test"))
                                .thenReturn(Optional.of(playerB));

                // 7. 模拟战役成员查询
                when(memberRepository.findByUser_IdAndCampaign_Id(2L, 1L))
                                .thenReturn(Optional.of(dmMember));

                when(memberRepository.findByUser_IdAndCampaign_Id(3L, 1L))
                                .thenReturn(Optional.of(memberA));

                when(memberRepository.findByUser_IdAndCampaign_Id(4L, 1L))
                                .thenReturn(Optional.of(memberB));

                // 8. 模拟备忘录查询
                when(memoRepository.findById(20L))
                                .thenReturn(Optional.of(privateMemo));

                when(memoRepository.findById(21L))
                                .thenReturn(Optional.of(publicMemo));
                when(memoRepository.save(any(Memo.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));
        }

@Test
@WithMockUser(username = "player_a_test")
void createMemo_shouldThrow_whenCampaignNotFound() {

    Long campaignId = 999L;

    // 构造一个用于通过成员身份检查的测试成员
    Campaign missingCampaign = new Campaign();
    missingCampaign.setId(campaignId);

    CampaignMember member = new CampaignMember();
    member.setId(100L);
    member.setUser(memberA.getUser());
    member.setCampaign(missingCampaign);
    member.setRole(CampaignMemberRole.PLAYER);

    // 允许权限检查通过
    when(memberRepository.findByUser_IdAndCampaign_Id(3L, campaignId))
            .thenReturn(Optional.of(member));

    // 但实际 Campaign 查询不存在
    when(campaignRepository.findById(campaignId))
            .thenReturn(Optional.empty());

    assertThrows(
            CampaignNotFoundException.class,
            () -> memoService.createMemo(
                    campaignId,
                    "测试Memo",
                    MemoCategory.NPC,
                    "测试描述",
                    false
            )
    );
}

        @Test
        @WithMockUser(username = "player_a_test")
        void memoPermissions() throws Exception {
                // 这里假设 memoId=20 属于玩家 B，且为私人备忘录
                when(memoRepository.findById(20L))
                                .thenReturn(Optional.of(privateMemo));

                mockMvc.perform(get("/api/memos/20")
                                .with(user("player_a_test")))
                                .andExpect(status().isForbidden());

                mockMvc.perform(get("/api/memos/20")
                                .with(user("dm_test")))
                                .andExpect(status().isOk());
        }

        @Test
        void playerCannotModifyOthersMemo() throws Exception {
                mockMvc.perform(delete("/api/memos/21")
                                .with(user("player_a_test"))
                                .with(csrf()))
                                .andExpect(status().isForbidden());

                verify(memoRepository, never()).delete(any(Memo.class));
        }

        @Test
        void dmCanDeletePrivateMemo() throws Exception {
                mockMvc.perform(delete("/api/memos/20")
                                .with(user("dm_test"))
                                .with(csrf()))
                                .andExpect(status().isNoContent());

                verify(memoRepository).delete(privateMemo);
        }

        @Test
@WithMockUser(username = "player_a_test")
void createMemo_shouldSave_whenCampaignExists() {

    Long campaignId = 1L;

    when(campaignRepository.findById(campaignId))
            .thenReturn(Optional.of(campaign));

    Memo result = memoService.createMemo(
            campaignId,
            "商业俱乐部",
            MemoCategory.ORGANIZATION,
            "测试描述",
            false
    );

    assertEquals("商业俱乐部", result.getName());
    assertEquals(MemoCategory.ORGANIZATION, result.getCategory());
    assertEquals(campaign, result.getCampaign());

    // 新增：验证创建者由服务器自动确定
    assertEquals(memberA, result.getOwnerMember());

    // 新增：验证公共状态
    assertEquals(false, result.isPrivate());

    verify(memoRepository).save(any(Memo.class));
}
}