package com.example.demo.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.MemoCategory;
import com.example.demo.Entity.Campaign;
import com.example.demo.Entity.Memo;
import com.example.demo.Repository.CampaignRepository;
import com.example.demo.Repository.MemoRepository;
import com.example.demo.exception.CampaignNotFoundException;
import com.example.demo.exception.MemoNotFoundException;

@Service
public class MemoService {
    private final MemoRepository memoRepository;
    private final CampaignRepository campaignRepository;

    /** 注入团务和备忘录数据仓库。 */
    public MemoService(CampaignRepository campaignRepository, MemoRepository memoRepository) {
        this.campaignRepository = campaignRepository;
        this.memoRepository = memoRepository;
    }

    /** 校验所属团务存在后创建备忘录。 */
    public Memo createMemo(
            Long campaignId,
            String name,
            MemoCategory category,
            String description) {
        Campaign campaign = campaignRepository.findById(campaignId)
    .orElseThrow(() -> new CampaignNotFoundException(campaignId));
        Memo memo = new Memo(campaign, name, category, description);
        memoRepository.save(memo);
        return memo;
    }

    /** 查询指定团务下的全部备忘录。 */
    public List<Memo> getMemosByCampaignId(Long campaignId) {
        return memoRepository.findByCampaignId(campaignId);
    }

    /** 按 ID 查询备忘录，不存在时抛出业务异常。 */
    public Memo getMemoById(Long memoId) {
    return memoRepository.findById(memoId).orElseThrow(() -> new MemoNotFoundException(memoId));
}

/** 更新指定备忘录的名称、分类和描述。 */
public Memo updateMemo(
    Long memoId,
    String name,
    MemoCategory category,
    String description
)
{
    Memo memo=memoRepository.findById(memoId).orElseThrow(() -> new MemoNotFoundException(memoId));
    memo.setName(name);
    memo.setCategory(category);
    memo.setDescription(description);
    memoRepository.save(memo);
    return memo;
}

/** 查询并删除指定备忘录，不存在时抛出业务异常。 */
public void deleteMemo(Long memoId) {
    Memo memo = memoRepository.findById(memoId).orElseThrow(() -> new MemoNotFoundException(memoId));
    memoRepository.delete(memo);
}

}
