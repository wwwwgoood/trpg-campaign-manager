package com.example.demo.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.Entity.NpcAffinity;

public interface NpcAffinityRepository extends JpaRepository<NpcAffinity, Long> {
    List<NpcAffinity> findByNpcCharacter_Id(Long characterId);

    void deleteByNpcCharacter_Id(Long characterId);

    void deleteByTargetCharacter_Id(Long characterId);
}
