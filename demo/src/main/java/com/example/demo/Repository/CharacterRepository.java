package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.demo.Entity.Character;

import jakarta.persistence.LockModeType;

public interface CharacterRepository extends JpaRepository<Character,Long> {
    /** 判断指定团务内是否已有同名角色。 */
    boolean existsByCampaign_IdAndName(Long campaignId,String name);
    List<Character> findByCampaign_Id(Long campaignId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT c FROM Character c WHERE c.id = :id")
Optional<Character> findByIdForUpdate(@Param("id") Long id);
}
