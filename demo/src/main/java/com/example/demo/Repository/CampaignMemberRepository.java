package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.Entity.CampaignMember;

public interface CampaignMemberRepository extends JpaRepository<CampaignMember,Long>{
    Optional<CampaignMember> findByUser_IdAndCampaign_Id(
        Long userId,
        Long campaignId
);

    Optional<CampaignMember> findByUser_Id(Long userId);
}
