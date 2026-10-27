package com.example.demo.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.Entity.Feat;

public interface FeatRepository extends JpaRepository<Feat,Long>{
    /** 按专长 ID 查询对应记录。 */
    List<Feat> findAllById(Long id);
}
