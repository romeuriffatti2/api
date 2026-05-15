package com.example.cert.controller;

import com.example.cert.Response.TemplateResponse;
import com.example.cert.service.templates.TemplateService;

import lombok.AllArgsConstructor;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/templates")
@AllArgsConstructor
public class SystemTemplateController {

    private final TemplateService templateService;

    @GetMapping
    public List<TemplateResponse> listSystemTemplates() {
        return templateService.listSystemTemplates();
    }
}
