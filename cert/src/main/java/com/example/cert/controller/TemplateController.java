package com.example.cert.controller;

import com.example.cert.Response.TemplateResponse;
import com.example.cert.domain.Usuario;
import com.example.cert.repository.UserRepository;
import com.example.cert.request.SaveTemplateRequest;
import com.example.cert.service.templates.TemplateService;

import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints para o usuário logado gerenciar seus próprios templates.
 * Acessível por CLIENT e ADMIN (cada um vê apenas os seus).
 */
@RestController
@RequestMapping("/api/my/templates")
@AllArgsConstructor
@CrossOrigin(origins = "*")
public class TemplateController {

    private final TemplateService templateService;
    private final UserRepository userRepository;

    private Usuario resolveUser(UserDetails principal) {
        return userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    public List<TemplateResponse> listMyTemplates(@AuthenticationPrincipal UserDetails principal) {
        return templateService.listMyTemplates(resolveUser(principal));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    public TemplateResponse getById(@PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        return templateService.getMyTemplateById(id, resolveUser(principal));
    }

    @GetMapping("/type/{type}")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    public TemplateResponse getByType(@PathVariable String type,
            @AuthenticationPrincipal UserDetails principal) {
        return templateService.getMyTemplateByType(type, resolveUser(principal));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    public TemplateResponse save(@PathVariable Long id,
            @RequestBody SaveTemplateRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        return templateService.update(id, req, resolveUser(principal));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    public TemplateResponse create(@RequestBody SaveTemplateRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        return templateService.create(req, resolveUser(principal));
    }

    @PostMapping("/{id}/clone")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    public TemplateResponse clone(@PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        return templateService.cloneMyTemplate(id, resolveUser(principal));
    }

    @PostMapping("/{id}/reset-to-default")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    public TemplateResponse resetToDefault(@PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        return templateService.resetToDefault(id, resolveUser(principal));
    }
}
