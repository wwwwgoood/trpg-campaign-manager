package com.example.demo.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.DTO.CreateMemoRequest;
import com.example.demo.DTO.MemoResponse;
import com.example.demo.DTO.UpdateMemoRequest;
import com.example.demo.Entity.Memo;
import com.example.demo.service.MemoService;

import jakarta.validation.Valid;

@RestController
public class MemoController {
    private final MemoService memoService;

    /** 注入备忘录服务。 */
    MemoController(MemoService memoService) {
        this.memoService = memoService;
    }

    /** 在指定团务下创建备忘录并返回响应数据。 */
    @PostMapping("/api/campaigns/{campaignId}/memos")
    public MemoResponse createMemo(
            @PathVariable Long campaignId,
            @Valid @RequestBody CreateMemoRequest request) {
        Memo memo = memoService.createMemo(
                campaignId,
                request.name(),
                request.category(),
                request.description());

        return toResponse(memo);
    }

    /** 查询指定团务的备忘录并逐条转换为响应。 */
    @GetMapping("/api/campaigns/{campaignId}/memos")
    public List<MemoResponse> getMemosByCampaignId(@PathVariable Long campaignId) {
        List<MemoResponse> memoResponses = new ArrayList<>();
        List<Memo> memos = memoService.getMemosByCampaignId(campaignId);
        for (Memo memo : memos) {
            memoResponses.add(toResponse(memo));
        }
        return memoResponses;
    }

    /** 按 ID 查询一条备忘录。 */
    @GetMapping("/api/memos/{memoId}")
    public MemoResponse getMemoById(@PathVariable Long memoId) {
        return toResponse(memoService.getMemoById(memoId));
    }

    /** 更新指定备忘录并返回更新后的内容。 */
    @PutMapping("/api/memos/{memoId}")
    public MemoResponse updateMemo(@PathVariable Long memoId, @Valid @RequestBody UpdateMemoRequest updateMemoRequest) {
        Memo memo = memoService.updateMemo(memoId, updateMemoRequest.name(), updateMemoRequest.category(),
                updateMemoRequest.description());
        return toResponse(memo);
    }

    /** 将备忘录实体映射为接口响应。 */
    private MemoResponse toResponse(Memo memo) {
        return new MemoResponse(
                memo.getId(),
                memo.getCampaign().getId(),
                memo.getName(),
                memo.getCategory(),
                memo.getDescription());
    }

    /** 删除指定备忘录，成功时返回 204。 */
    @DeleteMapping("/api/memos/{memoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)  
    public void deleteMemo(@PathVariable Long memoId) {
        memoService.deleteMemo(memoId);
    }
}
