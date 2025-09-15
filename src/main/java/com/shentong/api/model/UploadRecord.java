package com.shentong.api.model;

import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "upload_record")
@Data
public class UploadRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 仅用于 JPA，业务忽略

    @Column(name = "knowledge_id")
    private String knowledgeId;

    @Column(name = "knowledge_name")
    private String knowledgeName;

    @Column(name = "file_name")
    private String fileName;

}