package com.example.demo.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.demo.DTO.CharacterCreateRequest;
import com.example.demo.DTO.CharacterItemInput;
import com.example.demo.DTO.CharacterResponse;
import com.example.demo.DTO.CharacterSkillInput;
import com.example.demo.DTO.CharacterUpdateRequest;
import com.example.demo.DTO.NpcAffinityInput;
import com.example.demo.Entity.Campaign;
import com.example.demo.Entity.CampaignMember;
import com.example.demo.Entity.Character;
import com.example.demo.Entity.CharacterItem;
import com.example.demo.Entity.CharacterSkill;
import com.example.demo.Entity.Feat;
import com.example.demo.Entity.NpcAffinity;
import com.example.demo.Entity.Skill;
import com.example.demo.Repository.CampaignMemberRepository;
import com.example.demo.Repository.CampaignRepository;
import com.example.demo.Repository.CharacterItemRepository;
import com.example.demo.Repository.CharacterRepository;
import com.example.demo.Repository.CharacterSkillRepository;
import com.example.demo.Repository.FeatRepository;
import com.example.demo.Repository.NpcAffinityRepository;
import com.example.demo.Repository.SkillRepository;
import com.example.demo.enums.CampaignMemberRole;
import com.example.demo.enums.CharacterType;
import com.example.demo.enums.SkillType;
import com.example.demo.exception.BusinessRuleException;
import com.example.demo.exception.CampaignMemberNotFoundException;
import com.example.demo.exception.CampaignNotFoundException;
import com.example.demo.exception.CharacterNotFoundException;
import com.example.demo.mapper.CharacterMapper;

import jakarta.transaction.Transactional;

@Service
public class CharacterService {
        private final CampaignRepository campaignRepository;
        private final CampaignMemberRepository campaignMemberRepository;
        private final CharacterRepository characterRepository;
        private final SkillRepository skillRepository;
        private final CharacterItemRepository characterItemRepository;
        private final CharacterSkillRepository characterSkillRepository;
        private final FeatRepository featRepository;
        private final NpcAffinityRepository npcAffinityRepository;
        private CharacterMapper characterMapper;

        /** 注入角色创建、关联数据查询及响应转换所需的组件。 */
        public CharacterService(CampaignRepository campaignRepository,
                        CampaignMemberRepository campaignMemberRepository,
                        CharacterRepository characterRepository,
                        SkillRepository skillRepository,
                        CharacterItemRepository characterItemRepository,
                        FeatRepository featRepository,
                        CharacterSkillRepository characterSkillRepository,
                        CharacterMapper characterMapper,
                        NpcAffinityRepository npcAffinityRepository) {
                this.campaignRepository = campaignRepository;
                this.campaignMemberRepository = campaignMemberRepository;
                this.characterRepository = characterRepository;
                this.skillRepository = skillRepository;
                this.characterItemRepository = characterItemRepository;
                this.featRepository = featRepository;
                this.characterMapper = characterMapper;
                this.characterSkillRepository = characterSkillRepository;
                this.npcAffinityRepository = npcAffinityRepository;
        }

        /** 创建玩家角色及其技能、物品和专长，并按查看权限组装响应。 */
        @Transactional
        public CharacterResponse createPlayerCharacter(
                        Long campaignId,
                        CharacterCreateRequest request) {
                Campaign campaign = campaignRepository.findById(campaignId)
                                .orElseThrow(() -> new CampaignNotFoundException(campaignId));

                // 先校验请求完整性、归属关系、成员身份和角色名唯一性。
                validatePlayerCharacterRequest(request);
                CampaignMember ownerMember = campaignMemberRepository.findById(request.getOwnerMemberId())
                                .orElseThrow(() -> new CampaignMemberNotFoundException(request.getOwnerMemberId()));
                if (!ownerMember.getCampaign().getId().equals(campaignId)) {
                        throw new BusinessRuleException(
                                        "Campaign member does not belong to this campaign");
                }
                if (ownerMember.getRole() != CampaignMemberRole.PLAYER) {
                        throw new BusinessRuleException("Player character owner must have PLAYER role");
                }
                if (characterRepository.existsByCampaign_IdAndName(campaignId, request.getName())) {
                        throw new BusinessRuleException("Character name already existed");
                }

                // 将基础信息和私密字段写入角色实体；事务覆盖后续关联数据的保存。
                Character character = new Character();

                character.setCampaign(campaign);
                character.setOwnerMember(ownerMember);
                character.setCharacterType(CharacterType.PLAYER_CHARACTER);

                fillBasicFields(character, request);
                characterRepository.save(character);

                List<CharacterSkill> characterSkills = buildCharacterSkills(character, request.getSkills());
                characterSkillRepository.saveAll(characterSkills);

                // 将物品输入转换为角色物品实体。
                List<CharacterItem> items = buildCharacterItems(character, request.getItems());

                characterItemRepository.saveAll(items);

                // 校验专长 ID 并建立关联，同时拒绝玩家角色携带 NPC 亲密度。
                List<Feat> feats = resolveFeats(request);
                character.setFeats(feats);

                // 根据当前查看者的权限包装私密字段，再生成对外响应。
                boolean canViewPrivate = canViewPrivateFields(character, ownerMember);

                Integer maxHp = calculateMaxHp(character);

                return characterMapper.toResponse(character, characterSkills, items, List.of(), maxHp, canViewPrivate);

        }

        @Transactional
        public CharacterResponse createNpcCharacter(
                        Long campaignId,
                        CharacterCreateRequest request) {
                Campaign campaign = campaignRepository.findById(campaignId)
                                .orElseThrow(() -> new CampaignNotFoundException(campaignId));

                validateNpcCharacterRequest(request);

                if (characterRepository.existsByCampaign_IdAndName(
                                campaignId,
                                request.getName())) {
                        throw new BusinessRuleException(
                                        "Character name already exists in this campaign");
                }

                Character character = new Character();

                character.setCampaign(campaign);
                character.setOwnerMember(null);
                character.setCharacterType(CharacterType.NPC);

                fillBasicFields(character, request);

                List<CharacterSkill> characterSkills = buildCharacterSkills(character, request.getSkills());
                characterSkillRepository.saveAll(characterSkills);

                List<Feat> feats = resolveFeats(request);
                character.setFeats(feats);

                Character savedCharacter = characterRepository.save(character);

                List<CharacterItem> items = buildCharacterItems(character, request.getItems());
                characterItemRepository.saveAll(items);

                List<NpcAffinityInput> affinities = request.getAffinities();
                List<Long> targetIds;
                if (affinities == null || affinities.isEmpty()) {
                        targetIds = new ArrayList<>();
                } else {
                        targetIds = request.getAffinities()
                                        .stream()
                                        .map(NpcAffinityInput::getTargetCharacterId)
                                        .toList();
                }

                Set<Long> uniqueTargetIds = new HashSet<>(targetIds);

                if (uniqueTargetIds.size() != targetIds.size()) {
                        throw new BusinessRuleException(
                                        "Duplicate affinity targets are not allowed");
                }

                List<Character> targets = characterRepository.findAllById(uniqueTargetIds);

                if (targets.size() != uniqueTargetIds.size()) {
                        throw new BusinessRuleException(
                                        "Some affinity targets do not exist");
                }

                for (Character target : targets) {
                        if (!target.getCampaign().getId().equals(campaignId)) {
                                throw new BusinessRuleException(
                                                "Affinity target must belong to the same campaign");
                        }
                        if (target.getId().equals(savedCharacter.getId())) {
                                throw new BusinessRuleException(
                                                "NPC cannot have affinity toward itself");
                        }
                }

                Map<Long, Character> targetMap = targets.stream()
                                .collect(Collectors.toMap(
                                                Character::getId,
                                                target -> target));

                List<NpcAffinityInput> npcAffinityInputs = request.getAffinities() == null
                                ? List.of()
                                : request.getAffinities();

                List<NpcAffinity> npcAffinities = new ArrayList<>();

                for (NpcAffinityInput npcAffinityInput : npcAffinityInputs) {

                        Character target = targetMap.get(npcAffinityInput.getTargetCharacterId());

                        NpcAffinity npcAffinity = new NpcAffinity();

                        npcAffinity.setNpcCharacter(savedCharacter);
                        npcAffinity.setTargetCharacter(target);
                        npcAffinity.setIntimacy(npcAffinityInput.getIntimacy());

                        npcAffinities.add(npcAffinity);
                }

                npcAffinityRepository.saveAll(npcAffinities);

                Integer maxHp = calculateMaxHp(savedCharacter);
                boolean canViewPrivate = true;

                return characterMapper.toResponse(savedCharacter, characterSkills, items, npcAffinities, maxHp,
                                canViewPrivate);
        }

        private List<CharacterItem> buildCharacterItems(
                        Character character,
                        List<CharacterItemInput> inputs) {

                if (inputs == null || inputs.isEmpty()) {
                        return List.of();
                }

                List<CharacterItem> items = new ArrayList<>();

                for (CharacterItemInput input : inputs) {
                        CharacterItem item = new CharacterItem();

                        item.setCharacter(character);
                        item.setName(input.getName());
                        item.setQuantity(input.getQuantity());
                        item.setDescription(input.getDescription());

                        items.add(item);
                }

                return items;
        }

        private List<Feat> resolveFeats(CharacterCreateRequest request) {
                List<Feat> feats = new ArrayList<>();

                if (request.getFeatIds() != null && !request.getFeatIds().isEmpty()) {
                        Set<Long> uniqueFeatIds = new HashSet<>(request.getFeatIds());

                        if (uniqueFeatIds.size() != request.getFeatIds().size()) {
                                throw new BusinessRuleException(
                                                "Duplicate feats are not allowed");
                        }
                        feats = featRepository.findAllById(uniqueFeatIds);

                        if (feats.size() != uniqueFeatIds.size()) {
                                throw new BusinessRuleException(
                                                "Some feats do not exist");
                        }
                }
                return feats;
        }

        private void validateNpcCharacterRequest(CharacterCreateRequest request) {
                if (request.getOwnerMemberId() != null) {
                        throw new BusinessRuleException(
                                        "NPC cannot have an owner member");
                }
        }

        private void fillBasicFields(
                        Character character,
                        CharacterCreateRequest request) {
                character.setName(request.getName());
                character.setGender(request.getGender());
                character.setNationality(request.getNationality());
                character.setOccupation(request.getOccupation());
                character.setIdentity(request.getIdentity());

                character.setLevel(request.getLevel());
                character.setRank(request.getRank());

                character.setStr(request.getStr());
                character.setDex(request.getDex());
                character.setCon(request.getCon());
                character.setIntel(request.getIntel());
                character.setApp(request.getApp());
                character.setEdu(request.getEdu());

                character.setCurrentHp(request.getCurrentHp());

                character.setCashCents(request.getCashCents());
                character.setDebtCents(request.getDebtCents());
                character.setIncomeCents(request.getIncomeCents());
                character.setExpenseCents(request.getExpenseCents());

                character.setDesire(request.getDesire());
                character.setFear(request.getFear());
                character.setSecret(request.getSecret());
        }

        private List<CharacterSkill> buildCharacterSkills(
                        Character character,
                        List<CharacterSkillInput> inputs) {

                if (inputs == null || inputs.isEmpty()) {
                        return List.of();
                }

                // 检查技能重复或多余，再把请求中的技能值关联到数据库技能定义。
                List<SkillType> skillTypes = inputs
                                .stream()
                                .map(CharacterSkillInput::getSkillType)
                                .toList();

                Set<SkillType> uniqueTypes = new HashSet<>(skillTypes);

                if (uniqueTypes.size() != skillTypes.size()) {
                        throw new BusinessRuleException(
                                        "Duplicate skills are not allowed");
                } // 检查重复

                List<Skill> skillsFromDb = skillRepository.findAllByTypeIn(uniqueTypes);

                if (skillsFromDb.size() != uniqueTypes.size()) {
                        throw new BusinessRuleException(
                                        "Some skills do not exist");
                } // 检查有无多余

                Map<SkillType, Skill> skillMap = skillsFromDb.stream().collect(Collectors.toMap(
                                Skill::getType,
                                skill -> skill));

                List<CharacterSkill> characterSkills = new ArrayList<>();

                for (CharacterSkillInput input : inputs) {

                        Skill skill = skillMap.get(input.getSkillType());

                        CharacterSkill characterSkill = new CharacterSkill();

                        characterSkill.setCharacter(character);
                        characterSkill.setSkill(skill);
                        characterSkill.setValue(input.getValue());
                        characterSkill.setUnknown(input.isUnknown());

                        characterSkills.add(characterSkill);
                }

                return characterSkills;
        }

        private Integer calculateMaxHp(Character character) {

                if (character.getCon() == null
                                || character.getLevel() == null) {
                        return null;
                }

                return character.getCon() * 2
                                + character.getLevel() * 5;
        }

        /** 根据可见性包装字段，隐藏时不返回原始值。 */

        /** 判断查看者是否为 DM 或角色本人。 */
        private boolean canViewPrivateFields(
                        Character character,
                        CampaignMember viewerMember) {
                if (viewerMember.getRole() == CampaignMemberRole.DM) {
                        return true;
                }

                return character.getOwnerMember() != null
                                && character.getOwnerMember().getId()
                                                .equals(viewerMember.getId());
        }

        /** 校验玩家角色的必填字段和技能可见性约束。 */
        private void validatePlayerCharacterRequest(CharacterCreateRequest request) {

                if (request.getOwnerMemberId() == null) {
                        throw new BusinessRuleException(
                                        "Player character must have an owner");
                }

                // 玩家角色的基础属性、财务信息和背景信息必须完整。
                if (request.getGender() == null
                                || request.getNationality() == null
                                || request.getOccupation() == null
                                || request.getIdentity() == null
                                || request.getLevel() == null
                                || request.getRank() == null
                                || request.getStr() == null
                                || request.getDex() == null
                                || request.getCon() == null
                                || request.getIntel() == null
                                || request.getApp() == null
                                || request.getEdu() == null
                                || request.getCurrentHp() == null
                                || request.getCashCents() == null
                                || request.getDebtCents() == null
                                || request.getIncomeCents() == null
                                || request.getExpenseCents() == null
                                || request.getDesire() == null
                                || request.getFear() == null
                                || request.getSecret() == null) {

                        throw new BusinessRuleException(
                                        "Player character fields must be complete");
                }

                if (request.getSkills() == null || request.getSkills().isEmpty()) {
                        throw new BusinessRuleException(
                                        "Player character skills must be provided");
                }

                // 玩家角色不能提交标记为未知的技能值。
                for (CharacterSkillInput skill : request.getSkills()) {
                        if (skill.isUnknown()) {
                                throw new BusinessRuleException(
                                                "Player character skills cannot be unknown");
                        }
                }
                if (request.getAffinities() != null
                                && !request.getAffinities().isEmpty()) {
                        throw new BusinessRuleException(
                                        "Player character cannot have NPC affinities");
                }
        }

        @Transactional
        public CharacterResponse getCharacterResponse(
                        Long characterId,
                        Long viewerMemberId) {
                Character character = characterRepository.findById(characterId)
                                .orElseThrow(() -> new CharacterNotFoundException(characterId));
                CampaignMember viewerMember = campaignMemberRepository.findById(viewerMemberId)
                                .orElseThrow(() -> new CampaignMemberNotFoundException(viewerMemberId));
                if (!viewerMember.getCampaign().getId()
                                .equals(character.getCampaign().getId())) {

                        throw new BusinessRuleException(
                                        "Viewer does not belong to this campaign");
                }
                boolean canViewPrivate = canViewPrivateFields(character, viewerMember);
                List<CharacterSkill> skills = characterSkillRepository.findByCharacter_Id(character.getId());
                List<CharacterItem> items = characterItemRepository.findByCharacter_Id(character.getId());
                Integer maxHp = calculateMaxHp(character);
                List<NpcAffinity> affinities = npcAffinityRepository
                                .findByNpcCharacter_Id(character.getId());
                return characterMapper.toResponse(
                                character,
                                skills,
                                items,
                                affinities,
                                maxHp,
                                canViewPrivate);
        }

        @Transactional
        public List<CharacterResponse> getCharacters(
                        Long campaignId,
                        Long viewerMemberId) {

                // 1. Campaign 必须存在
                Campaign campaign = campaignRepository.findById(campaignId)
                                .orElseThrow(() -> new CampaignNotFoundException(campaignId));

                // 2. viewerMember 必须存在
                CampaignMember viewerMember = campaignMemberRepository.findById(viewerMemberId)
                                .orElseThrow(() -> new CampaignMemberNotFoundException(
                                                viewerMemberId));

                // 3. viewer 必须属于这个 Campaign
                if (!viewerMember.getCampaign().getId().equals(campaignId)) {
                        throw new BusinessRuleException(
                                        "Viewer does not belong to this campaign");
                }

                // 4. 查这个 Campaign 的所有 Character
                List<Character> characters = characterRepository.findByCampaign_Id(campaignId);

                // 5. 每个 Character 转成 CharacterResponse
                return characters.stream()
                                .map(character -> {

                                        List<CharacterSkill> skills = characterSkillRepository
                                                        .findByCharacter_Id(
                                                                        character.getId());

                                        List<CharacterItem> items = characterItemRepository
                                                        .findByCharacter_Id(
                                                                        character.getId());

                                        List<NpcAffinity> affinities = npcAffinityRepository
                                                        .findByNpcCharacter_Id(
                                                                        character.getId());

                                        boolean canViewPrivate = canViewPrivateFields(
                                                        character,
                                                        viewerMember);

                                        Integer maxHp = calculateMaxHp(character);

                                        return characterMapper.toResponse(
                                                        character,
                                                        skills,
                                                        items,
                                                        affinities,
                                                        maxHp,
                                                        canViewPrivate);
                                })
                                .toList();
        }

        @Transactional
        public void deleteCharacter(
                        Long characterId,
                        Long viewerMemberId) {

                // 1. Character 存在
                Character character = characterRepository.findById(characterId)
                                .orElseThrow(() -> new CharacterNotFoundException(characterId));

                // 2. viewer 存在
                CampaignMember viewerMember = campaignMemberRepository.findById(viewerMemberId)
                                .orElseThrow(() -> new CampaignMemberNotFoundException(
                                                viewerMemberId));

                // 3. 必须属于同一个 Campaign
                if (!viewerMember.getCampaign().getId()
                                .equals(character.getCampaign().getId())) {

                        throw new BusinessRuleException(
                                        "Viewer does not belong to this campaign");
                }

                // 4. 只有 DM 或 owner 能删除
                boolean isDm = viewerMember.getRole() == CampaignMemberRole.DM;

                boolean isOwner = character.getOwnerMember() != null
                                && character.getOwnerMember().getId()
                                                .equals(viewerMember.getId());

                if (!isDm && !isOwner) {
                        throw new BusinessRuleException(
                                        "Viewer is not allowed to delete this character");
                }

                // 5. 先删除所有引用 Character 的关联数据
                characterSkillRepository
                                .deleteByCharacter_Id(characterId);

                characterItemRepository
                                .deleteByCharacter_Id(characterId);

                npcAffinityRepository
                                .deleteByNpcCharacter_Id(characterId);

                npcAffinityRepository
                                .deleteByTargetCharacter_Id(characterId);

                // 6. 最后删除 Character
                characterRepository.delete(character);
        }

        @Transactional
        public CharacterResponse updateCharacter(
                        Long characterId,
                        Long viewerMemberId,
                        CharacterUpdateRequest request) {

                // 1. Character 存在
                Character character = characterRepository.findById(characterId)
                                .orElseThrow(() -> new CharacterNotFoundException(characterId));

                // 2. viewer 存在
                CampaignMember viewerMember = campaignMemberRepository.findById(viewerMemberId)
                                .orElseThrow(() -> new CampaignMemberNotFoundException(
                                                viewerMemberId));

                // 3. 同 Campaign
                if (!viewerMember.getCampaign().getId()
                                .equals(character.getCampaign().getId())) {

                        throw new BusinessRuleException(
                                        "Viewer does not belong to this campaign");
                }

                // 4. 只有 DM 或 owner 可以修改
                boolean isDm = viewerMember.getRole() == CampaignMemberRole.DM;

                boolean isOwner = character.getOwnerMember() != null
                                && character.getOwnerMember().getId()
                                                .equals(viewerMember.getId());

                if (!isDm && !isOwner) {
                        throw new BusinessRuleException(
                                        "Viewer is not allowed to update this character");
                }

                // 5. 如果改名字，检查 Campaign 内不能重名
                if (!character.getName().equals(request.getName())
                                && characterRepository.existsByCampaign_IdAndName(
                                                character.getCampaign().getId(),
                                                request.getName())) {

                        throw new BusinessRuleException(
                                        "Character name already exists");
                }

                // 6. 更新基础字段
                character.setName(request.getName());
                character.setGender(request.getGender());
                character.setNationality(request.getNationality());
                character.setOccupation(request.getOccupation());
                character.setIdentity(request.getIdentity());

                character.setLevel(request.getLevel());
                character.setRank(request.getRank());

                character.setStr(request.getStr());
                character.setDex(request.getDex());
                character.setCon(request.getCon());
                character.setIntel(request.getIntel());
                character.setApp(request.getApp());
                character.setEdu(request.getEdu());

                character.setCurrentHp(request.getCurrentHp());

                character.setCashCents(request.getCashCents());
                character.setDebtCents(request.getDebtCents());
                character.setIncomeCents(request.getIncomeCents());
                character.setExpenseCents(request.getExpenseCents());

                character.setDesire(request.getDesire());
                character.setFear(request.getFear());
                character.setSecret(request.getSecret());

                // 7. 保存
                Character savedCharacter = characterRepository.save(character);

                // 8. 读取关联数据
                List<CharacterSkill> skills = characterSkillRepository
                                .findByCharacter_Id(characterId);

                List<CharacterItem> items = characterItemRepository
                                .findByCharacter_Id(characterId);

                List<NpcAffinity> affinities = npcAffinityRepository
                                .findByNpcCharacter_Id(characterId);

                // 9. 派生字段 + 权限
                Integer maxHp = calculateMaxHp(savedCharacter);

                boolean canViewPrivate = canViewPrivateFields(
                                savedCharacter,
                                viewerMember);

                // 10. Response
                return characterMapper.toResponse(
                                savedCharacter,
                                skills,
                                items,
                                affinities,
                                maxHp,
                                canViewPrivate);
        }
}
