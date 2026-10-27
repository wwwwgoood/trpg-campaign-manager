package com.example.demo.Entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity 
@Getter 
@Setter 
public class Feat {
    @Id 
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long Id;

    private String name;
    private String description;
    
}
