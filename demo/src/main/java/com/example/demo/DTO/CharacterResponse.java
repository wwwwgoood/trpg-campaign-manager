package com.example.demo.DTO;

import java.util.List;

import com.example.demo.enums.CharacterType;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
public class CharacterResponse {
    private Long id;
    private Long campaignId;
    private CharacterType characterType;
    private Integer maxHp;

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

    private SensitiveFieldResponse<Long> cashCents;
    private SensitiveFieldResponse<Long> debtCents;
    private SensitiveFieldResponse<Long> incomeCents;
    private SensitiveFieldResponse<Long> expenseCents;
    private SensitiveFieldResponse<String> desire;
    private SensitiveFieldResponse<String> fear;
    private SensitiveFieldResponse<String> secret;
    private Long ownerMemberId;

    private List<CharacterSkillResponse> skills;
    private List<CharacterFeatResponse> feats;
    private List<CharacterItemResponse> items;
    private List<NpcAffinityResponse> affinities;
}
