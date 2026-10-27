package com.example.demo.Entity;

import com.example.demo.MemoCategory;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class Memo {

    @ManyToOne
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @Enumerated(EnumType.STRING)
    private MemoCategory category;

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

    /** 返回备忘录 ID。 */
    public Long getId() {
        return id;
    }

    /** 返回备忘录所属团务。 */
    public Campaign getCampaign() {
        return campaign;
    }

    /** 返回备忘录名称。 */
    public String getName() {
        return name;
    }

    /** 返回备忘录分类。 */
    public MemoCategory getCategory() {
        return category;
    }

    /** 返回备忘录描述。 */
    public String getDescription() {
        return description;
    }

    /** 设置备忘录所属团务。 */
    public void setCampaign(Campaign campaign) {
        this.campaign = campaign;
    }

    /** 设置备忘录名称。 */
    public void setName(String name) {
        this.name = name;
    }

    /** 设置备忘录分类。 */
    public void setCategory(MemoCategory category) {
        this.category = category;
    }

    /** 设置备忘录描述。 */
    public void setDescription(String description) {
        this.description = description;
    }
}
