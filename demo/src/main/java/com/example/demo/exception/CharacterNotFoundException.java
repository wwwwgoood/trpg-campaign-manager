package com.example.demo.exception;

public class CharacterNotFoundException extends RuntimeException{
    public CharacterNotFoundException(Long characterId)
    {
        super("Campaign not found: "+characterId);
    }
}
