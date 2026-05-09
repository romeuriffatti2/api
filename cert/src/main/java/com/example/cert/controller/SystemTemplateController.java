package com.example.cert.controller;

import com.example.cert.Response.TemplateResponse;
import com.example.cert.service.templates.TemplateService;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/system/templates")
@AllArgsConstructor
@CrossOrigin(origins = "*")
public class SystemTemplateController {

    private final TemplateService templateService;

    @GetMapping
    public List<TemplateResponse> listSystemTemplates() {
        return templateService.listSystemTemplates();
    }
}
