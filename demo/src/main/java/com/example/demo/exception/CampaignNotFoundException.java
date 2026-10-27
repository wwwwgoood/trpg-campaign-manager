package com.example.demo.exception;

public class CampaignNotFoundException extends RuntimeException {

    /** 使用缺失的团务 ID 构造异常信息。 */
    public CampaignNotFoundException(Long campaignId) {
        super("Campaign not found: " + campaignId);
    }
}
