package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.Entity.GameActionDefinition;

public interface GameActionDefinitionRepository
        extends JpaRepository<GameActionDefinition, Long> {
}