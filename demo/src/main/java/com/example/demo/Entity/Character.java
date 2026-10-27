package com.example.demo.Entity;

import java.util.ArrayList;
import java.util.List;

import com.example.demo.enums.CharacterType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Character {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne
    @JoinColumn(name = "owner_member_id")
    private CampaignMember ownerMember;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CharacterType characterType;

    @Column(nullable = false)
    private String name;
    private String gender;
    private String nationality;
    private String occupation;
    private String identity;

    private Integer level;
    private Integer rank;

    private Integer str;
    private Integer dex;
    private Integer con;
    private Integer intel;
    private Integer app;
    private Integer edu;

    private Integer currentHp;

    private Long cashCents;
    private Long debtCents;
    private Long incomeCents;
    private Long expenseCents;

    private String desire;
    private String fear;
    private String secret;

    @ManyToMany
    @JoinTable(name = "character_feat", joinColumns = @JoinColumn(name = "character_id"), inverseJoinColumns = @JoinColumn(name = "feat_id"))
    private List<Feat> feats = new ArrayList<>();

}
