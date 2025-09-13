package com.shentong.api.model;

import lombok.Data;

import javax.persistence.*;

@Entity
@Table(name = "name_relation")
@Data
public class NameRelation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 仅用于 JPA，业务忽略

    @Column(name = "name")
    private String name;

    @Column(name = "knowledge_id")
    private String knowledgeId;
}