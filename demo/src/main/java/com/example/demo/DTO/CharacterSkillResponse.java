package com.example.demo.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
public class CharacterSkillResponse {
    private Long skillId;
    private String name;
    private Integer value;
    private boolean unknown;
}
