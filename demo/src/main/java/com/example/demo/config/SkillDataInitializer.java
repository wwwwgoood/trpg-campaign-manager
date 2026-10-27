package com.example.demo.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.demo.Entity.Skill;
import com.example.demo.Repository.SkillRepository;
import com.example.demo.enums.SkillType;

@Configuration
public class SkillDataInitializer {
    /** 启动时补齐尚未写入数据库的技能类型。 */
    @Bean
    CommandLineRunner initSkills(SkillRepository skillRepository) {
        return args -> {
            // 按枚举逐个检查，避免重复插入已有技能定义。
            for (SkillType type : SkillType.values()) {
                if (skillRepository.findByType(type).isEmpty()) {
                    Skill skill = new Skill();
                    skill.setType(type);
                    skillRepository.save(skill);
                }
            }
        };
    }
}
