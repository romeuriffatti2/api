package com.example.cert.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Sprint 2 — Serviço de armazenamento de arquivos no disco local.
 * <p>
 * Salva imagens (backgrounds de templates, logos) em uma pasta configurável via
 * UPLOAD_DIR no .env. Os arquivos são acessíveis via HTTP por /uploads/** graças
 * ao WebMvcConfig registrado nesta sprint.
 * <p>
 * Na Sprint 3 (PDFME editor), o frontend usará POST /api/my/assets/image deste serviço
 * para fazer upload da imagem de fundo antes de salvar o template.
 */
@Slf4j
@Service
public class FileStorageService {

    private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB
    private static final List<String> ALLOWED_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/webp", "image/gif"
    );

    private final Path uploadRoot;

    public FileStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadRoot);
            log.info("FileStorageService inicializado. Diretório de upload: {}", this.uploadRoot);
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível criar o diretório de upload: " + this.uploadRoot, e);
        }
    }

    /**
     * Salva um arquivo MultipartFile no disco e retorna a URL pública relativa.
     *
     * @param file arquivo recebido via multipart/form-data
     * @return URL pública no formato "/uploads/{uuid}.{ext}"
     */
    public String store(MultipartFile file) {
        validate(file);

        String originalFilename = file.getOriginalFilename();
        String extension = extractExtension(originalFilename);
        String filename = UUID.randomUUID() + "." + extension;

        try {
            Path destination = this.uploadRoot.resolve(filename);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            log.info("Arquivo salvo: {}", destination);
            return "/uploads/" + filename;
        } catch (IOException e) {
            throw new RuntimeException("Falha ao salvar o arquivo: " + filename, e);
        }
    }

    /**
     * Remove um arquivo pelo nome (sem o prefixo /uploads/).
     *
     * @param filename nome do arquivo, ex: "uuid.png"
     */
    public void delete(String filename) {
        try {
            Path target = this.uploadRoot.resolve(filename).normalize();
            // Segurança: garante que o path não saiu da pasta de uploads (path traversal)
            if (!target.startsWith(this.uploadRoot)) {
                throw new SecurityException("Tentativa de acesso fora do diretório de upload.");
            }
            Files.deleteIfExists(target);
            log.info("Arquivo removido: {}", target);
        } catch (IOException e) {
            log.warn("Não foi possível remover o arquivo {}: {}", filename, e.getMessage());
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Arquivo está vazio.");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("Arquivo excede o tamanho máximo de 5 MB.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Tipo de arquivo não permitido: " + contentType);
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "bin";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
