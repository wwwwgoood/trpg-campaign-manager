package com.example.demo.dto;

import com.example.demo.Entity.Campaign;

public record CampaignResponse(
    Long id,
    String name
) {
    public static CampaignResponse from(Campaign campaign) {
        return new CampaignResponse(
            campaign.getId(),
            campaign.getName()
        );
    }
}