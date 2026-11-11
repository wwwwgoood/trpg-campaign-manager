package com.example.demo.dto;

import com.example.demo.enums.CampaignMemberRole;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CampaignMemberCreateRequest {
    @NotNull
private Long userId;
    private String displayName;
    private CampaignMemberRole role;
}