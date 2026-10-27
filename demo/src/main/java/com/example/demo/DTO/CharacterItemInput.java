package com.example.demo.DTO;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
public class CharacterItemInput {
    @NotBlank 
    private String name;

    @NotNull 
    @Min(1)
    private Integer quantity;

    private String description;
}
