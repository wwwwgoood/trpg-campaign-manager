package com.example.demo.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.example.demo.DTO.CharacterFeatResponse;
import com.example.demo.DTO.CharacterItemResponse;
import com.example.demo.DTO.CharacterResponse;
import com.example.demo.DTO.CharacterSkillResponse;
import com.example.demo.DTO.NpcAffinityResponse;
import com.example.demo.DTO.SensitiveFieldResponse;
import com.example.demo.Entity.Character;
import com.example.demo.Entity.CharacterItem;
import com.example.demo.Entity.CharacterSkill;
import com.example.demo.Entity.Feat;
import com.example.demo.Entity.NpcAffinity;

@Component
public class CharacterMapper {

    /** 将角色技能关联转换为技能响应。 */
    private CharacterSkillResponse toSkillResponse(CharacterSkill cs) {
        CharacterSkillResponse response = new CharacterSkillResponse();

        response.setSkillId(cs.getSkill().getId());
        response.setName(cs.getSkill().getType().name());
        response.setValue(cs.getValue());
        response.setUnknown(cs.isUnknown());

        return response;
    }

    /** 将角色物品转换为物品响应。 */
    private CharacterItemResponse toItemResponse(CharacterItem item) {
        CharacterItemResponse response = new CharacterItemResponse();

        response.setId(item.getId());
        response.setName(item.getName());
        response.setQuantity(item.getQuantity());
        response.setDescription(item.getDescription());

        return response;
    }

    /** 将专长实体转换为专长响应。 */
    private CharacterFeatResponse toFeatResponse(Feat feat) {
        CharacterFeatResponse response = new CharacterFeatResponse();

        response.setFeatId(feat.getId());
        response.setName(feat.getName());
        response.setDescription(feat.getDescription());

        return response;
    }

    /** 将 NPC 亲密度关系转换为响应。 */
    private NpcAffinityResponse toAffinityResponse(NpcAffinity affinity) {
        NpcAffinityResponse response = new NpcAffinityResponse();

        response.setId(affinity.getId());
        response.setTargetCharacterId(
                affinity.getTargetCharacter().getId());
        response.setTargetCharacterName(
                affinity.getTargetCharacter().getName());
        response.setIntimacy(affinity.getIntimacy());

        return response;
    }

    /**
     * 将 Character 及其关联数据转换为 CharacterResponse。
     *
     * Service 负责判断 canViewPrivate，
     * Mapper 负责根据这个结果构造敏感字段的响应格式。
     */
    public CharacterResponse toResponse(
            Character character,
            List<CharacterSkill> skills,
            List<CharacterItem> items,
            List<NpcAffinity> affinities,
            Integer maxHp,
            boolean canViewPrivate) {

        CharacterResponse response = new CharacterResponse();

        // 基础标识
        response.setId(character.getId());
        response.setCampaignId(character.getCampaign().getId());

        response.setOwnerMemberId(
                character.getOwnerMember() == null
                        ? null
                        : character.getOwnerMember().getId());

        response.setCharacterType(character.getCharacterType());

        // 基本信息
        response.setName(character.getName());
        response.setGender(character.getGender());
        response.setNationality(character.getNationality());
        response.setOccupation(character.getOccupation());
        response.setIdentity(character.getIdentity());

        // 等级与阶位
        response.setLevel(character.getLevel());
        response.setRank(character.getRank());

        // 六维
        response.setStr(character.getStr());
        response.setDex(character.getDex());
        response.setCon(character.getCon());
        response.setIntel(character.getIntel());
        response.setApp(character.getApp());
        response.setEdu(character.getEdu());

        // HP
        response.setCurrentHp(character.getCurrentHp());
        response.setMaxHp(maxHp);

        // 敏感财务信息
        response.setCashCents(
                sensitive(character.getCashCents(), canViewPrivate));

        response.setDebtCents(
                sensitive(character.getDebtCents(), canViewPrivate));

        response.setIncomeCents(
                sensitive(character.getIncomeCents(), canViewPrivate));

        response.setExpenseCents(
                sensitive(character.getExpenseCents(), canViewPrivate));

        // 敏感角色信息
        response.setDesire(
                sensitive(character.getDesire(), canViewPrivate));

        response.setFear(
                sensitive(character.getFear(), canViewPrivate));

        response.setSecret(
                sensitive(character.getSecret(), canViewPrivate));

        // 技能
        response.setSkills(
                skills == null
                        ? List.of()
                        : skills.stream()
                                .map(this::toSkillResponse)
                                .toList());

        // 物品
        response.setItems(
                items == null
                        ? List.of()
                        : items.stream()
                                .map(this::toItemResponse)
                                .toList());

        // 专长
        response.setFeats(
                character.getFeats() == null
                        ? List.of()
                        : character.getFeats()
                                .stream()
                                .map(this::toFeatResponse)
                                .toList());

        // NPC 亲密度
        response.setAffinities(
                affinities == null
                        ? List.of()
                        : affinities.stream()
                                .map(this::toAffinityResponse)
                                .toList());

        return response;
    }

    /** 根据可见性包装敏感字段。 */
    private <T> SensitiveFieldResponse<T> sensitive(
            T value,
            boolean visible) {

        SensitiveFieldResponse<T> response =
                new SensitiveFieldResponse<>();

        response.setVisible(visible);
        response.setValue(visible ? value : null);

        return response;
    }
}