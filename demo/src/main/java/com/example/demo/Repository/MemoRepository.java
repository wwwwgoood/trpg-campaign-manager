package com.example.demo.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.Entity.Memo;

public interface MemoRepository extends JpaRepository<Memo, Long> {

    /** 查询指定团务关联的备忘录。 */
    List<Memo> findByCampaignId(Long campaignId);
}
