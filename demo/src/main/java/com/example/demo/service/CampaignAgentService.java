
package com.example.demo.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.Entity.Campaign;
import com.example.demo.Entity.CampaignAgent;
import com.example.demo.Entity.CampaignMember;
import com.example.demo.enums.CampaignAgentType;
import com.example.demo.enums.CampaignMemberRole;
import com.example.demo.repository.CampaignAgentRepository;
import com.example.demo.repository.CampaignRepository;

@Service
public class CampaignAgentService {

    private final CampaignAgentRepository agentRepository;
    private final CampaignRepository campaignRepository;
    private final CampaignAccessService campaignAccessService;

    public CampaignAgentService(
            CampaignAgentRepository agentRepository,
            CampaignRepository campaignRepository,
            CampaignAccessService campaignAccessService) {

        this.agentRepository = agentRepository;
        this.campaignRepository = campaignRepository;
        this.campaignAccessService = campaignAccessService;
    }

    // 创建 AI 参与者
    @Transactional
    public CampaignAgent createAgent(
            Long campaignId,
            String name,
            CampaignAgentType agentType) {

        requireDM(campaignId);

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Agent name cannot be empty");
        }

        if (agentType == null) {
            throw new IllegalArgumentException(
                    "Agent type is required");
        }

        Campaign campaign = campaignRepository
                .findById(campaignId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Campaign not found"));

        CampaignAgent agent = new CampaignAgent();

        agent.setCampaign(campaign);
        agent.setName(name.trim());
        agent.setAgentType(agentType);
        agent.setEnabled(true);

        return agentRepository.save(agent);
    }

    // 查询战役的 AI 参与者
    @Transactional(readOnly = true)
    public List<CampaignAgent> getAgents(Long campaignId) {

        campaignAccessService.requireMember(campaignId);

        return agentRepository.findByCampaign_Id(campaignId);
    }

    // 启用或停用 AI
    @Transactional
    public void setEnabled(
            Long campaignId,
            Long agentId,
            boolean enabled) {

        requireDM(campaignId);

        CampaignAgent agent = agentRepository
                .findById(agentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Agent not found"));

        // 防止跨战役操作
        if (!agent.getCampaign().getId().equals(campaignId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Agent not found");
        }

        agent.setEnabled(enabled);
    }

    private void requireDM(Long campaignId) {

        CampaignMember member =
                campaignAccessService.requireMember(campaignId);

        if (member.getRole() != CampaignMemberRole.DM) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only DM can manage campaign agents");
        }
    }
}
