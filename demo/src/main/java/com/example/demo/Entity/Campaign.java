package com.example.demo.Entity;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
@Entity
public class Campaign {
    @Id
    @GeneratedValue
    private Long id;

    private String name;
    private String description;

    private LocalDate gameDate;

    /** 供 JPA 创建团务实体使用。 */
    public Campaign() {
    }

    /** 使用名称、描述和游戏日期初始化团务。 */
    public Campaign(String name, String description, LocalDate gameDate) {
        this.name = name;
        this.description = description;
        this.gameDate = gameDate;
    }

    /** 返回团务 ID。 */
    public Long getId() {
        return id;
    }

    /** 返回团务名称。 */
    public String getName() {
        return name;
    }

    /** 设置团务名称。 */
    public void setName(String name) {
        this.name = name;
    }

    /** 返回团务描述。 */
    public String getDescription() {
        return description;
    }

    /** 设置团务描述。 */
    public void setDescription(String description) {
        this.description = description;
    }

    /** 返回游戏日期。 */
    public LocalDate getGameDate() {
        return gameDate;
    }

    /** 设置游戏日期。 */
    public void setGameDate(LocalDate gameDate) {
        this.gameDate = gameDate;
    }
}
