package com.example.demo.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.CampaignResponse;
import com.example.demo.dto.CreateCampaignRequest;
import com.example.demo.service.CampaignService;

import jakarta.validation.Valid;

@RestController
public class CampaignController {
    private final CampaignService campaignService;

    /** 注入团务服务。 */
    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    /** 接收创建团务请求并返回保存后的团务。 */
@PostMapping("/api/campaigns")
public CampaignResponse createCampaign(
        @Valid @RequestBody CreateCampaignRequest request) {

    return CampaignResponse.from(
        campaignService.createCampaign(request.name())
    );
}

@GetMapping("/api/campaigns")
public List<CampaignResponse> getAllCampaigns() {
    return campaignService.getAllCampaigns()
            .stream()
            .map(CampaignResponse::from)
            .toList();
}

@GetMapping("/api/campaigns/{id}")
public ResponseEntity<CampaignResponse> getCampaignById(
        @PathVariable Long id) {

    return campaignService.getCampaignById(id)
            .map(CampaignResponse::from)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
}
}
