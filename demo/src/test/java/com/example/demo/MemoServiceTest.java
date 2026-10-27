package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.demo.Entity.Campaign;
import com.example.demo.Entity.Memo;
import com.example.demo.Repository.CampaignRepository;
import com.example.demo.Repository.MemoRepository;
import com.example.demo.exception.CampaignNotFoundException;
import com.example.demo.service.MemoService;

public class MemoServiceTest {

    private CampaignRepository campaignRepository;
    private MemoRepository memoRepository;
    private MemoService memoService;

    @BeforeEach
    void setUp() {
        campaignRepository = Mockito.mock(CampaignRepository.class);
        memoRepository = Mockito.mock(MemoRepository.class);

        memoService = new MemoService(
                campaignRepository,
                memoRepository);
    }

    @Test
    void createMemo_shouldThrow_whenCampaignNotFound() {
        // Arrange
        Long campaignId = 999L;

        when(campaignRepository.findById(campaignId))
                .thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(
                CampaignNotFoundException.class,
                () -> memoService.createMemo(
                        campaignId,
                        "测试Memo",
                        MemoCategory.NPC,
                        "测试描述"));
    }

    @Test
    void createMemo_shouldSave_whenCampaignExists() {
        Long campaignId = 1L;

        Campaign campaign = new Campaign(
                "1892芝加哥战役",
                "测试战役",
                LocalDate.of(1892, 10, 3));

        when(campaignRepository.findById(campaignId)).thenReturn(Optional.of(campaign));
        when(memoRepository.save(any(Memo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Memo result = memoService.createMemo(
                campaignId,
                "商业俱乐部",
                MemoCategory.ORGANIZATION,
                "测试描述");

        // Assert
        assertEquals("商业俱乐部", result.getName());
        assertEquals(MemoCategory.ORGANIZATION, result.getCategory());
        assertEquals(campaign, result.getCampaign());

        verify(memoRepository).save(any(Memo.class));
    }
}