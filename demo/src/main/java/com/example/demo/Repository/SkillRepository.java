package com.example.demo.Repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.Entity.Skill;
import com.example.demo.enums.SkillType;

public interface SkillRepository extends JpaRepository<Skill, Long> {
        /** 按一组技能类型查询技能定义。 */
        List<Skill> findAllByTypeIn(Collection<SkillType> types);

        /** 按技能类型查询已有定义。 */
        List<Skill> findByType(SkillType type);
}
