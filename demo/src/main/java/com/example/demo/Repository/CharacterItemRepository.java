package com.example.demo.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.Entity.CharacterItem;

public interface CharacterItemRepository
        extends JpaRepository<CharacterItem, Long> {
        List<CharacterItem> findByCharacter_Id(Long characterId);

        void deleteByCharacter_Id(Long characterId);
}
