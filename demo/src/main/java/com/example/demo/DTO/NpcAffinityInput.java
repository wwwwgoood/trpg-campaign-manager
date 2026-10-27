package com.example.demo.DTO;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
public class NpcAffinityInput {
    @NotNull 
    Long targetCharacterId;
    @NotNull 
    Integer intimacy;
}
