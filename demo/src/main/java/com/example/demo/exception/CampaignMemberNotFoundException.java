package com.example.demo.exception;

public class CampaignMemberNotFoundException extends RuntimeException {

    /** 使用缺失的成员 ID 构造异常信息。 */
    public CampaignMemberNotFoundException(Long id) {
        super("CampaignMember not found: " + id);
    }
}
