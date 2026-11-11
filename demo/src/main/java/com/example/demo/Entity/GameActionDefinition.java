package com.example.demo.Entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.example.demo.enums.ActionCostType;
import com.example.demo.enums.GameActionKind;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "game_action_definition")
@Getter
@Setter
public class GameActionDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameActionKind kind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionCostType costType = ActionCostType.NONE;

    @Min(0)
    @Column(nullable = false)
    private Integer costValue = 0;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String range;

    @Column(columnDefinition = "TEXT")
    private String supplement;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "instructions", columnDefinition = "jsonb")
    private String instructions;
}