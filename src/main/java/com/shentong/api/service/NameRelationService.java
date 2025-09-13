package com.shentong.api.service;


import com.shentong.api.model.NameRelation;
import com.shentong.api.repository.NameRelationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class NameRelationService {

    @Autowired
    private NameRelationRepository repository;

    // 插入数据
    public void save(String name, String knowledgeId) {
        if (name == null || knowledgeId == null) {
            return;
        }
        NameRelation relation = new NameRelation();
        relation.setName(name);
        relation.setKnowledgeId(knowledgeId);
        log.info("保存知识库ID 名称:{} -> ID: {}",name, knowledgeId);
        repository.save(relation);
    }

    // 查询所有数据
    public List<NameRelation> findAll() {
        return repository.findAll();
    }

    // 按 name 查询
    public List<NameRelation> findByName(String name) {
        return repository.findByName(name);
    }

    // 按 name 查询
    public String  getIdName(String name) {
        List<NameRelation> byName = repository.findByName(name);
        if (byName.isEmpty()){
            return null;
        }
        String knowledgeId = byName.get(0).getKnowledgeId();
        log.info("查询知识库ID 名称:{} -> ID: {}",name, knowledgeId);
        return  knowledgeId;
    }
}