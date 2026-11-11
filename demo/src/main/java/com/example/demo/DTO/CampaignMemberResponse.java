package com.example.demo.dto;

import com.example.demo.Entity.CampaignMember;
import com.example.demo.enums.CampaignMemberRole;

public record CampaignMemberResponse(
        Long id,
        Long campaignId,
        Long userId,
        String displayName,
        CampaignMemberRole role
) {
    public static CampaignMemberResponse from(CampaignMember member) {
        return new CampaignMemberResponse(
                member.getId(),
                member.getCampaign().getId(),
                member.getUser().getId(),
                member.getDisplayName(),
                member.getRole()
        );
    }
}
