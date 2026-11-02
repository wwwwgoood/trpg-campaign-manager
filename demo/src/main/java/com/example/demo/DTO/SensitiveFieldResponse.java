package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
public class SensitiveFieldResponse<T> {
    private boolean visible;
    private T value;
}
