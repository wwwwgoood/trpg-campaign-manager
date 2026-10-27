package com.example.demo.Entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(
    uniqueConstraints = {
        @UniqueConstraint(
            columnNames = {
                "npc_character_id",
                "target_character_id"
            }
        )
    }
)
public class NpcAffinity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(
        name = "npc_character_id",
        nullable = false
    )
    private Character npcCharacter;

    @ManyToOne
    @JoinColumn(
        name = "target_character_id",
        nullable = false
    )
    private Character targetCharacter;

    @Column(nullable = false)
    private Integer intimacy;
}