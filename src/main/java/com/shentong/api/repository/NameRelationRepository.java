package com.shentong.api.repository;

import com.shentong.api.model.NameRelation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NameRelationRepository extends JpaRepository<NameRelation, Long> {
    // 自定义查询方法（按 name 查询）
    List<NameRelation> findByName(String name);
}