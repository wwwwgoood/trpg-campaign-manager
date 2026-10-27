package com.example.demo.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NpcAffinityResponse {
    private Long id;
    private Long targetCharacterId;
    private String targetCharacterName;
    private Integer intimacy;
}
