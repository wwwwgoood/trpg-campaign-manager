package com.example.demo.service.tool;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.Entity.CampaignAgent;
import com.example.demo.Entity.CampaignMember;
import com.example.demo.enums.CampaignMemberRole;
import com.example.demo.repository.CampaignAgentRepository;
import com.example.demo.service.CampaignAccessService;

import tools.jackson.databind.JsonNode;

@Component
public class GameToolDispatcher {

        private final GameToolService gameToolService;
        private final CampaignAccessService campaignAccessService;
        private final CampaignAgentRepository agentRepository;

        public GameToolDispatcher(GameToolService gameToolService, CampaignAccessService campaignAccessService,
                        CampaignAgentRepository agentRepository) {
                this.gameToolService = gameToolService;
                this.campaignAccessService = campaignAccessService;
                this.agentRepository = agentRepository;
        }

        public Object dispatch(
                        ToolContext context,
                        String toolName,
                        JsonNode arguments) {

                if (context == null
                                || context.campaignId() == null
                                || context.userId() == null
                                || context.callerType() == null) {
                        throw new IllegalArgumentException(
                                        "Invalid tool context");
                }

                // 目前仅接通同步 HTTP 请求中的真人身份。
                // AI Agent 的授权将在后续单独实现。
                if (context.callerType() != ToolCallerType.HUMAN) {
                        throw new UnsupportedOperationException(
                                        "AI agent authorization is not implemented");
                }

                if (toolName == null || arguments == null
                                || !arguments.isObject()) {
                        throw new IllegalArgumentException(
                                        "Invalid tool call");
                }

                return switch (toolName) {

                        case "roll_dice" -> {
                                String expression = requireText(
                                                arguments, "expression");

                                yield gameToolService.rollDice(
                                                context, expression);
                        }

                        case "execute_action" -> {
                                Long actionId = requireLong(
                                                arguments, "actionId");

                                Long actorId = requireLong(
                                                arguments, "actorId");

                                Long targetId = optionalLong(
                                                arguments, "targetId");

                                yield gameToolService.executeAction(
                                                actionId, actorId, targetId);
                        }

                        default -> throw new IllegalArgumentException(
                                        "Unknown tool: " + toolName);
                };
        }

        private String requireText(JsonNode args, String field) {
                JsonNode value = args.get(field);

                if (value == null || !value.isTextual()
                                || value.asText().isBlank()) {
                        throw new IllegalArgumentException(
                                        "Invalid field: " + field);
                }

                return value.asText();
        }

        private Long requireLong(JsonNode args, String field) {
                JsonNode value = args.get(field);

                if (value == null || !value.isIntegralNumber()
                                || !value.canConvertToLong()
                                || value.longValue() <= 0) {
                        throw new IllegalArgumentException(
                                        "Invalid field: " + field);
                }

                return value.longValue();
        }

        private Long optionalLong(JsonNode args, String field) {
                JsonNode value = args.get(field);

                if (value == null || value.isNull()) {
                        return null;
                }

                return requireLong(args, field);
        }

        /**
         * 仅用于服务器内部 AI 工具循环的无状态工具。
         * 不代表 AI 已获得战役或角色操作权限。
         */
        public Object dispatchStateless(
                        String toolName,
                        JsonNode arguments) {

                if (toolName == null
                                || arguments == null
                                || !arguments.isObject()) {
                        throw new IllegalArgumentException(
                                        "Invalid tool call");
                }

                return switch (toolName) {

                        case "roll_dice" -> {
                                String expression = requireText(
                                                arguments, "expression");

                                yield gameToolService.rollDiceStateless(
                                                expression);
                        }

                        default -> throw new IllegalArgumentException(
                                        "Tool is not permitted in stateless mode: "
                                                        + toolName);
                };
        }

        public Object dispatchAgent(
                        AgentPrincipal principal,
                        String toolName,
                        JsonNode arguments) {

                if (principal == null
                                || toolName == null
                                || arguments == null
                                || !arguments.isObject()) {
                        throw new IllegalArgumentException(
                                        "Invalid agent tool call");
                }

                requireAgentLaunch(principal);

                return switch (toolName) {

                        case "roll_dice" -> {
                                String expression = requireText(arguments, "expression");

                                yield gameToolService
                                                .rollDiceStateless(expression);
                        }

                        case "execute_action" -> {
                                Long actionId = requireLong(arguments, "actionId");

                                Long actorId = requireLong(arguments, "actorId");

                                Long targetId = optionalLong(arguments, "targetId");

                                yield gameToolService.executeActionAsAgent(
                                                principal,
                                                actionId,
                                                actorId,
                                                targetId);
                        }

                        default -> throw new IllegalArgumentException(
                                        "Unknown AI tool: " + toolName);
                };
        }

        public void requireAgentLaunch(AgentPrincipal principal) {

                if (principal == null) {
                        throw new IllegalArgumentException(
                                        "Agent principal is required");
                }

                // 1. 验证当前真人属于该战役，而且是 DM
                CampaignMember member = campaignAccessService.requireMember(
                                principal.campaignId());

                if (member.getRole() != CampaignMemberRole.DM) {
                        throw new ResponseStatusException(
                                        HttpStatus.FORBIDDEN,
                                        "Only DM can launch AI agents");
                }

                // 2. 从数据库查询 AI Agent
                CampaignAgent agent = agentRepository
                                .findById(principal.agentId())
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.FORBIDDEN,
                                                "Agent not found"));

                // 3. 验证战役归属
                if (!principal.campaignId().equals(
                                agent.getCampaign().getId())) {
                        throw new ResponseStatusException(
                                        HttpStatus.FORBIDDEN,
                                        "Agent belongs to another campaign");
                }

                // 4. 验证启用状态
                if (!agent.isEnabled()) {
                        throw new ResponseStatusException(
                                        HttpStatus.FORBIDDEN,
                                        "Agent is disabled");
                }
        }

}