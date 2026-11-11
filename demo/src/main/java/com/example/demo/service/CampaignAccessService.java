package com.example.demo.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.Entity.CampaignMember;
import com.example.demo.Entity.Character;
import com.example.demo.Entity.User;
import com.example.demo.enums.CampaignMemberRole;
import com.example.demo.repository.CampaignMemberRepository;
import com.example.demo.repository.UserRepository;

@Service
public class CampaignAccessService {

        private final UserRepository userRepository;
        private final CampaignMemberRepository memberRepository;

        public CampaignAccessService(
                        UserRepository userRepository,
                        CampaignMemberRepository memberRepository) {

                this.userRepository = userRepository;
                this.memberRepository = memberRepository;
        }

        public CampaignMember requireMember(Long campaignId) {
                // 找到campaign，找不到就异常
                // 获取上下文
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

                // 2. 检查是否登录
                if (authentication == null ||
                                !authentication.isAuthenticated() ||
                                authentication instanceof AnonymousAuthenticationToken) {

                        throw new ResponseStatusException(
                                        HttpStatus.UNAUTHORIZED,
                                        "Not authenticated");
                }

                // 3. 根据认证信息查询用户
                User user = userRepository
                                .findByUsername(authentication.getName())
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.UNAUTHORIZED,
                                                "User not found"));

                // 4. 查询当前用户在指定战役中的成员身份
                return memberRepository
                                .findByUser_IdAndCampaign_Id(user.getId(), campaignId)
                                .orElseThrow(() -> new ResponseStatusException(
                                                HttpStatus.FORBIDDEN,
                                                "You are not a member of this campaign"));
        }

        public void requireCharacterEditor(
                        Character character,
                        CampaignMember member) {
                if (!member.getCampaign().getId()
                                .equals(character.getCampaign().getId())) {

                        throw new ResponseStatusException(
                                        HttpStatus.FORBIDDEN,
                                        "You do not belong to this campaign");
                }

                // 2. 判断是否为 DM
                boolean isDM = member.getRole() == CampaignMemberRole.DM;

                // 3. 判断是否为角色拥有者
                boolean isOwner = character.getOwnerMember() != null
                                && character.getOwnerMember().getId()
                                                .equals(member.getId());

                // 4. 既不是 DM，也不是拥有者
                if (!isDM && !isOwner) {

                        throw new ResponseStatusException(
                                        HttpStatus.FORBIDDEN,
                                        "You cannot edit this character");
                }
        }

        public CampaignMember requireMemberForUser(
        Long campaignId,
        Long userId) {

    if (campaignId == null || userId == null) {
        throw new IllegalArgumentException(
                "Campaign ID and User ID are required"
        );
    }

    return memberRepository
            .findByUser_IdAndCampaign_Id(userId, campaignId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "User is not a member of this campaign"
            ));
}
        
}
