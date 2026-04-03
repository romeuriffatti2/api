package com.example.cert.controller;

import com.example.cert.Response.TemplateResponse;
import com.example.cert.request.SaveTemplateRequest;
import com.example.cert.service.TemplateService;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints administrativos para gerenciar os templates padrão do sistema.
 * Somente ADMIN pode listar e editar os templates padrão via este controller.
 */
@RestController
@RequestMapping("/api/admin/templates/system")
@AllArgsConstructor
@CrossOrigin(origins = "*")
public class TemplateAdminController {

    private final TemplateService templateService;

    /** Lista todos os templates padrão do sistema */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<TemplateResponse> listSystemTemplates() {
        return templateService.listSystemTemplates();
    }

    /** Atualiza um template padrão do sistema (afeta futuros resets) */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public TemplateResponse updateSystemTemplate(@PathVariable Long id,
                                                  @RequestBody SaveTemplateRequest req) {
        return templateService.updateSystemTemplate(id, req);
    }
}
