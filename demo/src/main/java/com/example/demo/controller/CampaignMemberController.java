package com.example.demo.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.DTO.CampaignMemberCreateRequest;
import com.example.demo.Entity.CampaignMember;
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
    public CampaignMember createMember(
            @PathVariable Long campaignId,
            @RequestBody CampaignMemberCreateRequest request) {

        return campaignMemberService.createMember(
                campaignId,
                request
        );
    }
}
