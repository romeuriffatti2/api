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
 * Endpoints para o usuário gerenciar os templates associados às suas revistas.
 * Acessível por CLIENT e ADMIN.
 */
@RestController
@RequestMapping("/api/my/templates")
@AllArgsConstructor
public class TemplateController {

    private final TemplateService templateService;
    private final UserRepository userRepository;

    private Usuario resolveUser(UserDetails principal) {
        return userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }

    @GetMapping("/magazine/{magazineId}")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    public List<TemplateResponse> listTemplatesByMagazine(@PathVariable Long magazineId,
            @AuthenticationPrincipal UserDetails principal) {
        return templateService.listTemplatesByMagazine(magazineId, resolveUser(principal));
    }

    @GetMapping("/magazine/{magazineId}/{id}")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    public TemplateResponse getById(@PathVariable Long magazineId, @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        return templateService.getTemplateByIdAndMagazine(id, magazineId, resolveUser(principal));
    }

    @GetMapping("/magazine/{magazineId}/type/{type}")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    public TemplateResponse getByType(@PathVariable Long magazineId, @PathVariable String type,
            @AuthenticationPrincipal UserDetails principal) {
        return templateService.getTemplateByTypeAndMagazine(type, magazineId, resolveUser(principal));
    }

    @PutMapping("/magazine/{magazineId}/{id}")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    public TemplateResponse save(@PathVariable Long magazineId, @PathVariable Long id,
            @RequestBody SaveTemplateRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        return templateService.update(id, magazineId, req, resolveUser(principal));
    }

    @PostMapping("/magazine/{magazineId}")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    public TemplateResponse create(@PathVariable Long magazineId,
            @RequestBody SaveTemplateRequest req,
            @AuthenticationPrincipal UserDetails principal) {
        return templateService.create(magazineId, req, resolveUser(principal));
    }

    @PostMapping("/magazine/{magazineId}/{id}/clone")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    public TemplateResponse clone(@PathVariable Long magazineId, @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        return templateService.cloneTemplate(id, magazineId, resolveUser(principal));
    }

    @PostMapping("/magazine/{magazineId}/{id}/reset-to-default")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    public TemplateResponse resetToDefault(@PathVariable Long magazineId, @PathVariable Long id,
            @AuthenticationPrincipal UserDetails principal) {
        return templateService.resetToDefault(id, magazineId, resolveUser(principal));
    }
}
