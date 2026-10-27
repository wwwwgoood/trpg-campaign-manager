package com.example.demo.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.Entity.CampaignMember;

public interface CampaignMemberRepository extends JpaRepository<CampaignMember,Long>{

}
