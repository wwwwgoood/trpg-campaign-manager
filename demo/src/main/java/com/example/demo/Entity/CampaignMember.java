package com.example.demo.Entity;

import com.example.demo.enums.CampaignMemberRole;

import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import jakarta.persistence.EnumType;

@Entity
@Data 
public class CampaignMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Campaign campaign;

    private String displayName;

    @Enumerated(EnumType.STRING)
    private CampaignMemberRole role;
}
