package com.shentong.api.service;

import com.shentong.api.model.UploadRecord;
import com.shentong.api.repository.UploadRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UploadRecordService {

    private final UploadRecordRepository uploadRecordRepository;

    @Autowired
    public UploadRecordService(UploadRecordRepository uploadRecordRepository) {
        this.uploadRecordRepository = uploadRecordRepository;
    }

    
    public UploadRecord save(UploadRecord uploadRecord) {
        return uploadRecordRepository.save(uploadRecord);
    }

    
    public Optional<UploadRecord> findById(Long id) {
        return uploadRecordRepository.findById(id);
    }

    
    public List<UploadRecord> findByKnowledgeId(String knowledgeId) {
        return uploadRecordRepository.findByKnowledgeId(knowledgeId);
    }

    
    public List<UploadRecord> findByFileNameContaining(String fileName) {
        return uploadRecordRepository.findByFileNameContaining(fileName);
    }

    
    public Optional<UploadRecord> findByKnowledgeIdAndFileName(String knowledgeId, String fileName) {
        return uploadRecordRepository.findByKnowledgeIdAndFileName(knowledgeId, fileName);
    }

    
    public long countByKnowledgeId(String knowledgeId) {
        return uploadRecordRepository.countByKnowledgeId(knowledgeId);
    }

    
    public void deleteById(Long id) {
        uploadRecordRepository.deleteById(id);
    }
}