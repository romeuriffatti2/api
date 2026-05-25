package com.example.cert.controller;

import com.example.cert.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Endpoint para manipulação de assets físicos do sistema (como imagens de template).
 */
@RestController
@RequestMapping("/api/my/assets")
@RequiredArgsConstructor
public class AssetController {

    private final FileStorageService fileStorageService;

    @PostMapping("/image")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        String relativeUrl = fileStorageService.store(file);
        return ResponseEntity.ok(Map.of("url", relativeUrl));
    }
}
