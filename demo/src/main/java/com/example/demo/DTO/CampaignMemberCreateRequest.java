package com.example.demo.dto;

import com.example.demo.enums.CampaignMemberRole;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CampaignMemberCreateRequest {
    private String displayName;
    private CampaignMemberRole role;
}