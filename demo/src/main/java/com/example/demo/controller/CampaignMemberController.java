package com.example.demo.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.CampaignMemberCreateRequest;
import com.example.demo.dto.CampaignMemberResponse;
import com.example.demo.service.CampaignMemberService;

@RestController
@RequestMapping("/api")
public class CampaignMemberController {
    
    private final CampaignMemberService campaignMemberService;

    public CampaignMemberController(CampaignMemberService campaignMemberService)
    {
        this.campaignMemberService=campaignMemberService;
    }

    @PostMapping("/campaigns/{campaignId}/members")
    public CampaignMemberResponse createMember(
            @PathVariable Long campaignId,
            @RequestBody CampaignMemberCreateRequest request) {

return CampaignMemberResponse.from(
    campaignMemberService.createMember(campaignId, request)
);
    }
}
