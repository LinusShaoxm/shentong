package com.shentong.api.controller;


import com.shentong.api.model.NameRelation;
import com.shentong.api.service.NameRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/h2data")
public class NameRelationController {

    @Autowired
    private NameRelationService service;

    @PostMapping
    public void save(@RequestParam String name, @RequestParam String knowledgeId) {
        service.save(name, knowledgeId);
    }

    @GetMapping
    public List<NameRelation> findAll() {
        return service.findAll();
    }

    @GetMapping("/search")
    public List<NameRelation> findByName(@RequestParam String name) {
        return service.findByName(name);
    }
}