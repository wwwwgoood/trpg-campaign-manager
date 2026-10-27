package com.example.demo.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.Entity.Campaign;
import com.example.demo.service.CampaignService;

@RestController
public class CampaignController {
    private final CampaignService campaignService;

    /** 注入团务服务。 */
    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    /** 接收创建团务请求并返回保存后的团务。 */
    @PostMapping("/api/campaigns")
    public Campaign createCampaign(@RequestBody Campaign campaign) {
        return campaignService.createCampaign(campaign);
    }

    /** 返回所有团务。 */
    @GetMapping("/api/campaigns")
    public List<Campaign> getallCampaigns()
    {
        return campaignService.getAllCampaigns();
    }

    /** 按 ID 查询团务，找不到时返回空结果。 */
    @GetMapping("/api/campaigns/{id}")
    public Optional<Campaign> getCampaignById(@PathVariable Long id)
    {
        return campaignService.getCampaignById(id);
    }
}
