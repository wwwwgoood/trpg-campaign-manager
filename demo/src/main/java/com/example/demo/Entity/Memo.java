package com.example.demo.Entity;

import com.example.demo.MemoCategory;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Setter 
@Getter 
public class Memo {

    @ManyToOne
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @Enumerated(EnumType.STRING)
    private MemoCategory category;

    private boolean isPrivate = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_member_id")
    private CampaignMember ownerMember;

    @Id
    @GeneratedValue
    private Long id;

    private String name;
    private String description;

    /** 供 JPA 创建备忘录实体使用。 */
    public Memo() {
    }

    /** 使用所属团务、名称、分类和描述初始化备忘录。 */
    public Memo(
            Campaign campaign,
            String name,
            MemoCategory category,
            String description) {
        this.campaign = campaign;
        this.name = name;
        this.category = category;
        this.description = description;
    }

}
