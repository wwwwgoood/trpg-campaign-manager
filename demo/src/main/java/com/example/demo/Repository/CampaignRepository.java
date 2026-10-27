package com.example.demo.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.Entity.Campaign;

public interface CampaignRepository extends JpaRepository<Campaign,Long>{

}
