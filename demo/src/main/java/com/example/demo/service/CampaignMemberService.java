package com.example.demo.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.Entity.Campaign;
import com.example.demo.Entity.CampaignMember;
import com.example.demo.Entity.User;
import com.example.demo.dto.CampaignMemberCreateRequest;
import com.example.demo.enums.CampaignMemberRole;
import com.example.demo.exception.CampaignNotFoundException;
import com.example.demo.repository.CampaignMemberRepository;
import com.example.demo.repository.CampaignRepository;
import com.example.demo.repository.UserRepository;

@Service
public class CampaignMemberService {

    private final CampaignRepository campaignRepository;
    private final CampaignMemberRepository campaignMemberRepository;
    private final CampaignAccessService campaignAccessService;
private final UserRepository userRepository;

public CampaignMemberService(
        CampaignRepository campaignRepository,
        CampaignMemberRepository campaignMemberRepository,
        CampaignAccessService campaignAccessService,
        UserRepository userRepository) {

    this.campaignRepository = campaignRepository;
    this.campaignMemberRepository = campaignMemberRepository;
    this.campaignAccessService = campaignAccessService;
    this.userRepository = userRepository;
}

@Transactional
public CampaignMember createMember(
        Long campaignId,
        CampaignMemberCreateRequest request) {

    CampaignMember currentMember =
            campaignAccessService.requireMember(campaignId);

    if (currentMember.getRole() != CampaignMemberRole.DM) {
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Only DM can add campaign members"
        );
    }

    Campaign campaign = campaignRepository.findById(campaignId)
            .orElseThrow(() ->
                    new CampaignNotFoundException(campaignId));

    User user = userRepository.findById(request.getUserId())
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "User not found"
            ));

    if (campaignMemberRepository
            .findByUser_IdAndCampaign_Id(user.getId(), campaignId)
            .isPresent()) {

        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "User is already a campaign member"
        );
    }

    CampaignMember member = new CampaignMember();
    member.setCampaign(campaign);
    member.setUser(user);
    member.setDisplayName(request.getDisplayName());
    member.setRole(CampaignMemberRole.PLAYER);

    return campaignMemberRepository.save(member);
}
}
