package com.example.demo.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CharacterUpdateRequest {

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
}
