package com.example.cert.controller;

import com.example.cert.Response.TemplateResponse;
import com.example.cert.service.TemplateService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoint público para leitura dos templates padrão do sistema.
 * Clientes podem ver (mas não editar) os templates padrão para usar como referência.
 */
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
