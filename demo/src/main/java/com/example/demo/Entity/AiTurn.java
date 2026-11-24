
package com.example.demo.Entity;

import java.time.Instant;

import com.example.demo.enums.AiTurnStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ai_turn")
@Getter
@Setter
public class AiTurn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long campaignId;

    @Column(nullable = false)
    private Long agentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AiTurnStatus status = AiTurnStatus.RUNNING;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String messagesJson;

    @Column(nullable = false)
    private Integer roundCount = 0;

    @Column(nullable = false)
    private Integer toolCallCount = 0;

    @Column(columnDefinition = "TEXT")
    private String finalResponse;

    @Version
    private Long version;

    @Column(name = "lease_owner", length = 100)
    private String leaseOwner;

    @Column(name = "lease_until")
    private Instant leaseUntil;
}
