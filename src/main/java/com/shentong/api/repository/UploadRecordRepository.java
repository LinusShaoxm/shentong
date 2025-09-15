package com.shentong.api.repository;

import com.shentong.api.model.UploadRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UploadRecordRepository extends JpaRepository<UploadRecord, Long> {
    
    // 根据 knowledgeId 查询记录
    List<UploadRecord> findByKnowledgeId(String knowledgeId);
    
    // 根据 fileName 查询记录（模糊查询）
    List<UploadRecord> findByFileNameContaining(String fileName);
    
    // 根据 knowledgeId 和 fileName 查询
    Optional<UploadRecord> findByKnowledgeIdAndFileName(String knowledgeId, String fileName);
    
    // 统计某个 knowledgeId 的记录数
    long countByKnowledgeId(String knowledgeId);
}