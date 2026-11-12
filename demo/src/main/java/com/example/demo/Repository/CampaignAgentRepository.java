package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.Entity.CampaignAgent;
import com.example.demo.enums.CampaignAgentType;

public interface CampaignAgentRepository
        extends JpaRepository<CampaignAgent, Long> {

    List<CampaignAgent> findByCampaign_Id(Long campaignId);

    List<CampaignAgent> findByCampaign_IdAndEnabledTrue(
            Long campaignId);

    Optional<CampaignAgent> findByIdAndEnabledTrue(Long id);

    List<CampaignAgent>
            findByCampaign_IdAndAgentTypeAndEnabledTrue(
                    Long campaignId,
                    CampaignAgentType agentType);
}