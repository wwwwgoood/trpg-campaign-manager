package com.example.demo.service.tool;

public sealed interface ExecutionPrincipal
        permits HumanPrincipal, AgentPrincipal {

    Long campaignId();
}