
package com.example.demo.service.character;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.Entity.CampaignAgent;
import com.example.demo.Entity.Character;
import com.example.demo.repository.CampaignAgentRepository;
import com.example.demo.service.tool.AgentPrincipal;

@Service
public class AgentCharacterAccessService {

        private final CampaignAgentRepository agentRepository;

        public AgentCharacterAccessService(
                        CampaignAgentRepository agentRepository) {
                this.agentRepository = agentRepository;
        }

        public boolean canViewPrivate(
                        AgentPrincipal principal,
                        Character character) {

                if (principal == null || character == null) {
                        throw new IllegalArgumentException(
                                        "Agent and character are required");
                }

                // 1. 从数据库读取真实的 AI Agent
                CampaignAgent agent = agentRepository
                                .findById(principal.agentId())
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.FORBIDDEN,
                                                "Agent not found"));

                // 2. 校验 AI 身份和启用状态
                if (!agent.isEnabled()
                                || !agent.getCampaign().getId()
                                                .equals(principal.campaignId())) {

                        throw new ResponseStatusException(
                                        HttpStatus.FORBIDDEN,
                                        "Agent is not authorized");
                }

                // 3. 禁止跨战役读取
                if (!character.getCampaign().getId()
                                .equals(principal.campaignId())) {

                        throw new ResponseStatusException(
                                        HttpStatus.FORBIDDEN,
                                        "Character belongs to another campaign");
                }

                // 4. 根据 Agent 类型判断字段可见性
                return switch (agent.getAgentType()) {

                        case AI_DM -> true;

                        case AI_PLAYER ->
                                character.getOwnerAgent() != null
                                                && character.getOwnerAgent().getId()
                                                                .equals(principal.agentId());
                };
        }

        public void requireControl(
                        AgentPrincipal principal,
                        Character character) {

                if (principal == null || character == null) {
                        throw new IllegalArgumentException(
                                        "Agent principal and character are required");
                }

                // 1. 验证角色属于 Agent 所声明的战役
                Long campaignId = character.getCampaign().getId();

                if (!campaignId.equals(principal.campaignId())) {
                        throw new ResponseStatusException(
                                        HttpStatus.FORBIDDEN,
                                        "Character belongs to another campaign");
                }

                // 2. 从数据库查询 Agent，不能只相信 principal 中的 ID
                CampaignAgent agent = agentRepository
                                .findById(principal.agentId())
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.FORBIDDEN,
                                                "Agent is not available"));

                // 3. 检查 Agent 的实际战役归属及启用状态
                if (!campaignId.equals(agent.getCampaign().getId())
                                || !agent.isEnabled()) {
                        throw new ResponseStatusException(
                                        HttpStatus.FORBIDDEN,
                                        "Agent is not authorized");
                }

                // 4. 根据 Agent 类型检查角色控制权
                boolean allowed = switch (agent.getAgentType()) {

                        case AI_PLAYER ->
                                character.getOwnerAgent() != null
                                                && principal.agentId().equals(
                                                                character.getOwnerAgent().getId());

                        case AI_DM ->
                                character.getOwnerMember() == null
                                                && character.getOwnerAgent() == null;
                };

                if (!allowed) {
                        throw new ResponseStatusException(
                                        HttpStatus.FORBIDDEN,
                                        "Agent cannot control this character");
                }
        }
}
