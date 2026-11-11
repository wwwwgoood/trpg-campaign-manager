package com.example.demo.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.MemoCategory;
import com.example.demo.Entity.Campaign;
import com.example.demo.Entity.CampaignMember;
import com.example.demo.Entity.Memo;
import com.example.demo.enums.CampaignMemberRole;
import com.example.demo.exception.CampaignNotFoundException;
import com.example.demo.exception.MemoNotFoundException;
import com.example.demo.repository.CampaignRepository;
import com.example.demo.repository.MemoRepository;

import jakarta.transaction.Transactional;

@Service
public class MemoService {
    private final MemoRepository memoRepository;
    private final CampaignRepository campaignRepository;
    private final CampaignAccessService campaignAccessService;

    /** 注入团务和备忘录数据仓库。 */
    public MemoService(CampaignRepository campaignRepository, MemoRepository memoRepository,
            CampaignAccessService campaignAccessService) {
        this.campaignRepository = campaignRepository;
        this.memoRepository = memoRepository;
        this.campaignAccessService = campaignAccessService;
    }

    /** 校验所属团务存在后创建备忘录。 */
    @Transactional
    public Memo createMemo(
            Long campaignId,
            String name,
            MemoCategory category,
            String description,
            boolean isPrivate) {

        CampaignMember currentMember = campaignAccessService.requireMember(campaignId);

        Campaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new CampaignNotFoundException(campaignId));

        Memo memo = new Memo(campaign, name, category, description);

        memo.setOwnerMember(currentMember);
        memo.setPrivate(isPrivate);

        return memoRepository.save(memo);
    }

    /** 查询指定团务下的全部备忘录。 */
    public List<Memo> getMemosByCampaignId(Long campaignId) {

        CampaignMember currentMember = campaignAccessService.requireMember(campaignId);

        boolean isDM = currentMember.getRole() == CampaignMemberRole.DM;

        return memoRepository.findByCampaignId(campaignId)
                .stream()
                .filter(memo -> {
                    // 公共备忘录所有战役成员都能看到
                    if (!memo.isPrivate()) {
                        return true;
                    }

                    // DM 能查看所有私人备忘录
                    if (isDM) {
                        return true;
                    }

                    // 普通玩家只能查看自己的私人备忘录
                    return memo.getOwnerMember() != null
                            && memo.getOwnerMember().getId()
                                    .equals(currentMember.getId());
                })
                .toList();
    }

    /** 按 ID 查询备忘录，不存在时抛出业务异常。 */
    public Memo getMemoById(Long memoId) {

        Memo memo = memoRepository.findById(memoId)
                .orElseThrow(() -> new MemoNotFoundException(memoId));

        CampaignMember currentMember = campaignAccessService.requireMember(
                memo.getCampaign().getId());

        boolean isDM = currentMember.getRole() == CampaignMemberRole.DM;

        boolean isOwner = memo.getOwnerMember() != null
                && memo.getOwnerMember().getId()
                        .equals(currentMember.getId());

        if (memo.isPrivate() && !isDM && !isOwner) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot view this private memo");
        }

        return memo;
    }

    /** 更新指定备忘录的名称、分类和描述。 */
    @Transactional
    public Memo updateMemo(
            Long memoId,
            String name,
            MemoCategory category,
            String description) {

        Memo memo = memoRepository.findById(memoId)
                .orElseThrow(() -> new MemoNotFoundException(memoId));

        // 修改前检查权限
        requireMemoEditor(memo);

        memo.setName(name);
        memo.setCategory(category);
        memo.setDescription(description);

        return memoRepository.save(memo);
    }

    /** 查询并删除指定备忘录，不存在时抛出业务异常。 */
    @Transactional
    public void deleteMemo(Long memoId) {

        Memo memo = memoRepository.findById(memoId)
                .orElseThrow(() -> new MemoNotFoundException(memoId));

        requireMemoEditor(memo);

        memoRepository.delete(memo);
    }

    private void requireMemoEditor(Memo memo) {

        CampaignMember currentMember = campaignAccessService.requireMember(
                memo.getCampaign().getId());

        boolean isDM = currentMember.getRole() == CampaignMemberRole.DM;

        boolean isOwner = memo.getOwnerMember() != null
                && memo.getOwnerMember().getId()
                        .equals(currentMember.getId());

        if (!isDM && !isOwner) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot modify this memo");
        }
    }

}
