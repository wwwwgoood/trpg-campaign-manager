package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.Entity.AiTurn;

public interface AiTurnRepository
        extends JpaRepository<AiTurn, Long> {
}