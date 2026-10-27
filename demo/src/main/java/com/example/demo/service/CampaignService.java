package com.example.demo.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.demo.Entity.Campaign;
import com.example.demo.Repository.CampaignRepository;

@Service
public class CampaignService {

    private final CampaignRepository campaignRepository;

    /** 注入团务数据仓库。 */
    public CampaignService(CampaignRepository campaignRepository) {
        this.campaignRepository = campaignRepository;
    }

    /** 保存并返回新团务。 */
    public Campaign createCampaign(Campaign campaign)
    {
        return campaignRepository.save(campaign);
    }
    /** 查询全部团务。 */
    public List<Campaign> getAllCampaigns()
    {
        return campaignRepository.findAll();
    }

    /** 按 ID 查询团务，结果可能为空。 */
    public Optional<Campaign> getCampaignById(Long id)
    {
        return campaignRepository.findById(id);
    }
}
