
package com.example.demo.service.action;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.Entity.CampaignMember;
import com.example.demo.Entity.Character;
import com.example.demo.Entity.GameActionDefinition;
import com.example.demo.enums.ActionCostType;
import com.example.demo.enums.CampaignMemberRole;
import com.example.demo.repository.CharacterRepository;
import com.example.demo.service.CampaignAccessService;

@Service
public class JpaActionRuntime implements ActionRuntime {

    private final CharacterRepository characterRepository;
    private final CampaignAccessService campaignAccessService;

    public JpaActionRuntime(
            CharacterRepository characterRepository,
            CampaignAccessService campaignAccessService) {
        this.characterRepository = characterRepository;
        this.campaignAccessService = campaignAccessService;
    }

    @Override
    public void requireCanExecute(
            Long actorId,
            Long targetId,
            GameActionDefinition action) {

        if (action == null || action.getId() == null) {
            throw new IllegalArgumentException("Action is required");
        }

        // 锁定行动角色，直到外层事务结束
        Character actor = requireLockedCharacter(actorId);

        Long campaignId = actor.getCampaign().getId();

        // 根据登录身份确认当前用户属于该战役
        CampaignMember member =
                campaignAccessService.requireMember(campaignId);

        boolean isDM =
                member.getRole() == CampaignMemberRole.DM;

        boolean isOwner =
                actor.getOwnerMember() != null
                && actor.getOwnerMember().getId()
                        .equals(member.getId());

        if (!isDM && !isOwner) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot control this character");
        }

        // 若指定目标，必须属于同一战役
        if (targetId != null) {
            Character target = requireCharacter(targetId);

            if (!campaignId.equals(
                    target.getCampaign().getId())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Target belongs to another campaign");
            }
        }

        // 校验行动掌握情况
        boolean knowsAction = actor.getKnownActions()
                .stream()
                .anyMatch(known ->
                        action.getId().equals(known.getId()));

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

        Character character =
                requireLockedCharacter(characterId);

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

        Character character =
                requireLockedCharacter(characterId);

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
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Character not found"));
    }

    private Character requireLockedCharacter(Long id) {
        return characterRepository.findByIdForUpdate(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
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
}
