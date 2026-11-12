
package com.example.demo.service.action;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.Entity.CampaignAgent;
import com.example.demo.Entity.CampaignMember;
import com.example.demo.Entity.Character;
import com.example.demo.Entity.GameActionDefinition;
import com.example.demo.enums.ActionCostType;
import com.example.demo.enums.CampaignMemberRole;
import com.example.demo.repository.CampaignAgentRepository;
import com.example.demo.repository.CharacterRepository;
import com.example.demo.service.CampaignAccessService;
import com.example.demo.service.tool.AgentPrincipal;

@Service
public class JpaActionRuntime implements ActionRuntime {

        private final CharacterRepository characterRepository;
        private final CampaignAccessService campaignAccessService;
        private final CampaignAgentRepository agentRepository;

        public JpaActionRuntime(
                        CharacterRepository characterRepository,
                        CampaignAccessService campaignAccessService, CampaignAgentRepository agentRepository) {
                this.characterRepository = characterRepository;
                this.campaignAccessService = campaignAccessService;
                this.agentRepository = agentRepository;
        }

        @Override
        public void requireCanExecute(
                        Long actorId,
                        Long targetId,
                        GameActionDefinition action) {

                requireValidAction(action);

                // 先锁定 actor
                Character actor = requireLockedCharacter(actorId);

                // 1. 检查真人是否有权控制角色
                requireHumanCanControl(actor);

                // 2. 检查与调用者身份无关的游戏规则
                requireGameRules(actor, targetId, action);
        }

        private void requireValidAction(GameActionDefinition action) {

                if (action == null || action.getId() == null) {
                        throw new IllegalArgumentException(
                                        "Action is required");
                }
        }

        private void requireHumanCanControl(Character actor) {

                Long campaignId = actor.getCampaign().getId();

                CampaignMember member = campaignAccessService.requireMember(campaignId);

                boolean isDM = member.getRole() == CampaignMemberRole.DM;

                boolean isOwner = actor.getOwnerMember() != null
                                && actor.getOwnerMember().getId()
                                                .equals(member.getId());

                if (!isDM && !isOwner) {
                        throw new ResponseStatusException(
                                        HttpStatus.FORBIDDEN,
                                        "You cannot control this character");
                }
        }

        private void requireGameRules(
                        Character actor,
                        Long targetId,
                        GameActionDefinition action) {

                Long campaignId = actor.getCampaign().getId();

                // 检查目标是否属于同一战役
                if (targetId != null) {
                        Character target = requireCharacter(targetId);

                        if (!campaignId.equals(
                                        target.getCampaign().getId())) {
                                throw new ResponseStatusException(
                                                HttpStatus.FORBIDDEN,
                                                "Target belongs to another campaign");
                        }
                }

                // 检查角色是否掌握行动
                boolean knowsAction = actor.getKnownActions()
                                .stream()
                                .anyMatch(known -> action.getId().equals(known.getId()));

                if (!knowsAction) {
                        throw new ResponseStatusException(
                                        HttpStatus.FORBIDDEN,
                                        "Character has not learned this action");
                }
        }

        @Override
        public void consumeActionCost(
                        Long actorId,
                        GameActionDefinition action) {

                Character actor = requireLockedCharacter(actorId);

                ActionCostType type = action.getCostType();
                Integer cost = action.getCostValue();

                if (type == null || cost == null || cost < 0) {
                        throw new IllegalArgumentException(
                                        "Invalid action cost");
                }

                switch (type) {

                        case NONE -> {
                                if (cost != 0) {
                                        throw new IllegalArgumentException(
                                                        "NONE cost must be zero");
                                }
                        }

                        case AP -> {
                                actor.setCurrentAp(
                                                subtractResource(
                                                                actor.getCurrentAp(), cost));
                        }

                        case RP -> {
                                actor.setCurrentRp(
                                                subtractResource(
                                                                actor.getCurrentRp(), cost));
                        }

                        default -> throw new UnsupportedOperationException(
                                        "Unsupported action cost: " + type);
                }
        }

        @Override
        public void changeHp(Long characterId, int delta) {

                Character character = requireLockedCharacter(characterId);

                Integer hp = character.getCurrentHp();

                if (hp == null) {
                        throw new IllegalStateException(
                                        "Character HP is not initialized");
                }

                int newHp = Math.addExact(hp, delta);

                // HP 不能低于零
                character.setCurrentHp(Math.max(0, newHp));
        }

        @Override
        public void changeResource(
                        Long characterId,
                        String resource,
                        int delta) {

                Character character = requireLockedCharacter(characterId);

                if (resource == null) {
                        throw new IllegalArgumentException(
                                        "Resource is required");
                }

                switch (resource) {

                        case "AP" -> character.setCurrentAp(
                                        addResource(
                                                        character.getCurrentAp(), delta));

                        case "RP" -> character.setCurrentRp(
                                        addResource(
                                                        character.getCurrentRp(), delta));

                        default -> throw new UnsupportedOperationException(
                                        "Unsupported resource: " + resource);
                }
        }

        @Override
        public void applyStatus(
                        Long characterId,
                        String status) {

                // 尚未实现状态效果的持久化模型
                throw new UnsupportedOperationException(
                                "Status effects are not implemented yet");
        }

        private Character requireCharacter(Long id) {
                return characterRepository.findById(id)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Character not found"));
        }

        private Character requireLockedCharacter(Long id) {
                return characterRepository.findByIdForUpdate(id)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Character not found"));
        }

        private int subtractResource(Integer current, int cost) {

                if (current == null || current < 0) {
                        throw new IllegalStateException(
                                        "Character resource is not initialized");
                }

                if (cost < 0 || current < cost) {
                        throw new IllegalArgumentException(
                                        "Insufficient resource");
                }

                return current - cost;
        }

        private int addResource(Integer current, int delta) {

                if (current == null || current < 0) {
                        throw new IllegalStateException(
                                        "Character resource is not initialized");
                }

                int result = Math.addExact(current, delta);

                if (result < 0) {
                        throw new IllegalArgumentException(
                                        "Insufficient resource");
                }

                return result;
        }

        @Override
        public void requireCanExecute(
                        AgentPrincipal principal,
                        Long actorId,
                        Long targetId,
                        GameActionDefinition action) {

                if (principal == null) {
                        throw new IllegalArgumentException(
                                        "Agent principal is required");
                }

                requireValidAction(action);

                Character actor = requireLockedCharacter(actorId);

                // 必须先确认 actor 属于所声明的战役
                Long campaignId = actor.getCampaign().getId();

                if (!campaignId.equals(principal.campaignId())) {
                        throw new ResponseStatusException(
                                        HttpStatus.FORBIDDEN,
                                        "Actor belongs to another campaign");
                }

                CampaignAgent agent = agentRepository
                                .findById(principal.agentId())
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.FORBIDDEN,
                                                "Agent is not available"));

                // Agent 必须存在于当前战役且处于启用状态
                if (!campaignId.equals(agent.getCampaign().getId())
                                || !agent.isEnabled()) {
                        throw new ResponseStatusException(
                                        HttpStatus.FORBIDDEN,
                                        "Agent is not authorized");
                }

                boolean allowed = switch (agent.getAgentType()) {

                        case AI_PLAYER ->
                                actor.getOwnerAgent() != null
                                                && principal.agentId().equals(
                                                                actor.getOwnerAgent().getId());

                        case AI_DM ->
                                actor.getOwnerMember() == null
                                                && actor.getOwnerAgent() == null;
                };

                if (!allowed) {
                        throw new ResponseStatusException(
                                        HttpStatus.FORBIDDEN,
                                        "Agent cannot control this character");
                }

                // AI 和真人必须遵守相同的游戏规则
                requireGameRules(actor, targetId, action);
        }

}
