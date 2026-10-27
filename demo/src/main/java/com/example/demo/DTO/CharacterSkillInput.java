package com.example.demo.DTO;

import com.example.demo.enums.SkillType;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CharacterSkillInput {

    @NotNull
    private SkillType skillType;

    private Integer value;

    private boolean unknown;
}
