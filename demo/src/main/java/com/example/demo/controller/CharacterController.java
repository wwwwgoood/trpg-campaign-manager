package com.example.demo.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.DTO.CharacterCreateRequest;
import com.example.demo.DTO.CharacterResponse;
import com.example.demo.DTO.CharacterUpdateRequest;
import com.example.demo.service.CharacterService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class CharacterController {
    private final CharacterService characterService;

    public CharacterController(CharacterService characterService) {
        this.characterService = characterService;
    }

    @PostMapping("/campaigns/{campaignId}/player-characters")
    public CharacterResponse createPlayerCharacter(
            @PathVariable Long campaignId,
            @Valid @RequestBody CharacterCreateRequest request) {

        return characterService.createPlayerCharacter(
                campaignId,
                request);
    }

    @PostMapping("/campaigns/{campaignId}/npcs")
    public CharacterResponse createNpcCharacter(
            @PathVariable Long campaignId,
            @Valid @RequestBody CharacterCreateRequest request) {

        return characterService.createNpcCharacter(
                campaignId,
                request);
    }

    @GetMapping("/characters/{characterId}")
    public CharacterResponse getCharacter(
            @PathVariable Long characterId,
            @RequestParam Long viewerMemberId) {

        return characterService.getCharacterResponse(
                characterId,
                viewerMemberId);
    }

    @GetMapping("/campaigns/{campaignId}/characters")
    public List<CharacterResponse> getCharacters(
            @PathVariable Long campaignId,
            @RequestParam Long viewerMemberId) {

        return characterService.getCharacters(
                campaignId,
                viewerMemberId);
    }

    @DeleteMapping("/characters/{characterId}")
    public void deleteCharacter(
            @PathVariable Long characterId,
            @RequestParam Long viewerMemberId) {

        characterService.deleteCharacter(
                characterId,
                viewerMemberId);
    }

    @PutMapping("/characters/{characterId}")
    public CharacterResponse updateCharacter(
            @PathVariable Long characterId,
            @RequestParam Long viewerMemberId,
            @RequestBody CharacterUpdateRequest request) {

        return characterService.updateCharacter(
                characterId,
                viewerMemberId,
                request);
    }
}
