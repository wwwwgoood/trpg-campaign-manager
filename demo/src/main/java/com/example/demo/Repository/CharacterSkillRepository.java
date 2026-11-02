package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.Entity.CharacterSkill;

public interface CharacterSkillRepository extends JpaRepository<CharacterSkill,Long>{
    List<CharacterSkill> findByCharacter_Id(Long characterId);

    void deleteByCharacter_Id(Long characterId);
}
