package com.example.demo.service;

import org.springframework.stereotype.Service;

import com.example.demo.Entity.Campaign;
import com.example.demo.Entity.CampaignMember;
import com.example.demo.dto.CampaignMemberCreateRequest;
import com.example.demo.exception.CampaignNotFoundException;
import com.example.demo.repository.CampaignMemberRepository;
import com.example.demo.repository.CampaignRepository;

@Service
public class CampaignMemberService {

    private final CampaignRepository campaignRepository;
    private final CampaignMemberRepository campaignMemberRepository;

    public CampaignMemberService(
            CampaignRepository campaignRepository,
            CampaignMemberRepository campaignMemberRepository) {
        this.campaignRepository = campaignRepository;
        this.campaignMemberRepository = campaignMemberRepository;
    }

    public CampaignMember createMember(
            Long campaignId,
            CampaignMemberCreateRequest request) {

        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() ->
                        new CampaignNotFoundException(campaignId));

        CampaignMember member = new CampaignMember();
        member.setCampaign(campaign);
        member.setDisplayName(request.getDisplayName());
        member.setRole(request.getRole());

        return campaignMemberRepository.save(member);
    }
}
