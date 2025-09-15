package com.shentong.api.controller;

import com.shentong.api.model.UploadRecord;
import com.shentong.api.service.UploadRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/record")
public class UploadRecordController {

    private final UploadRecordService uploadRecordService;

    @Autowired
    public UploadRecordController(UploadRecordService uploadRecordService) {
        this.uploadRecordService = uploadRecordService;
    }

    @PostMapping
    public UploadRecord create(@RequestBody UploadRecord uploadRecord) {
        return uploadRecordService.save(uploadRecord);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UploadRecord> getById(@PathVariable Long id) {
        Optional<UploadRecord> record = uploadRecordService.findById(id);
        return record.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<UploadRecord> getByKnowledgeId(@RequestParam String knowledgeId) {
        return uploadRecordService.findByKnowledgeId(knowledgeId);
    }

    @GetMapping("/search")
    public List<UploadRecord> searchByFileName(@RequestParam String fileName) {
        return uploadRecordService.findByFileNameContaining(fileName);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        uploadRecordService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}