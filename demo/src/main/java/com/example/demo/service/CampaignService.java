package com.example.demo.service;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.Entity.Campaign;
import com.example.demo.Entity.CampaignMember;
import com.example.demo.Entity.User;
import com.example.demo.enums.CampaignMemberRole;
import com.example.demo.repository.CampaignMemberRepository;
import com.example.demo.repository.CampaignRepository;
import com.example.demo.repository.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final CampaignMemberRepository campaignMemberRepository;
    private final UserRepository userRepository;
    private final CampaignAccessService campaignAccessService;

    /** 注入团务数据仓库。 */
    public CampaignService(
            CampaignRepository campaignRepository,
            CampaignMemberRepository campaignMemberRepository,
            UserRepository userRepository, CampaignAccessService campaignAccessService) {

        this.campaignRepository = campaignRepository;
        this.campaignMemberRepository = campaignMemberRepository;
        this.userRepository = userRepository;
        this.campaignAccessService = campaignAccessService;
    }

    /** 保存并返回新团务。 */
@Transactional
public Campaign createCampaign(String name) {

    User currentUser = requireCurrentUser();

    Campaign campaign = new Campaign();
    campaign.setName(name);

    Campaign savedCampaign = campaignRepository.save(campaign);

    CampaignMember member = new CampaignMember();
    member.setCampaign(savedCampaign);
    member.setUser(currentUser);
    member.setRole(CampaignMemberRole.DM);
    member.setDisplayName(currentUser.getUsername());

    campaignMemberRepository.save(member);

    return savedCampaign;
}

    /** 查询全部团务。 */
    public List<Campaign> getAllCampaigns() {

        User currentUser = requireCurrentUser();

        return campaignMemberRepository
                .findByUser_Id(currentUser.getId())
                .stream()
                .map(CampaignMember::getCampaign)
                .toList();
    }

    /** 按 ID 查询团务，结果可能为空。 */
    public Optional<Campaign> getCampaignById(Long id) {

        campaignAccessService.requireMember(id);

        return campaignRepository.findById(id);
    }

    private User requireCurrentUser() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Not authenticated");
        }

        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "User not found"));
    }
}
