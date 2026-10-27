package com.example.demo.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.Entity.Character;

public interface CharacterRepository extends JpaRepository<Character,Long> {
    /** 判断指定团务内是否已有同名角色。 */
    boolean existsByCampaign_IdAndName(Long campaignId,String name);
    List<Character> findByCampaign_Id(Long campaignId);
}
