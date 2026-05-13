package com.example.cert.service;

import com.example.cert.Response.CertificateResponse;
import lombok.RequiredArgsConstructor;
import jakarta.annotation.PostConstruct;
import com.example.cert.domain.Certificate;
import com.example.cert.domain.CertificateTemplate;
import com.example.cert.domain.Magazine;
import com.example.cert.mapper.CertificateMapper;
import com.example.cert.repository.CertificateRepository;
import com.example.cert.repository.CertificateTemplateRepository;
import com.example.cert.repository.MagazineRepository;
import com.example.cert.request.CertificateRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CertificateService {

    private final CertificateRepository certificateRepository;
    private final CertificateMapper certificateMapper;
    private final MagazineRepository magazineRepository;
    private final CertificateTemplateRepository certificateTemplateRepository;
    private final CertificateEmailService certificateEmailService;
    @Value("${app.certificate.storage.path}")
    private String storagePath;

    @PostConstruct
    public void init() {
        // Garante que o diretório de armazenamento existe
        try {
            Files.createDirectories(Paths.get(storagePath));
        } catch (IOException e) {
            throw new RuntimeException("Não foi possível criar o diretório de armazenamento de certificados", e);
        }
    }

    public Page<CertificateResponse> getAllCertificates(Pageable pageable) {
        return certificateRepository.findAll(pageable).map(certificateMapper::toResponse);
    }

    @Transactional
    public void create(CertificateRequest certificateRequest) {

        Magazine magazine = magazineRepository.findById(certificateRequest.getMagazineId()).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Revista não encontrada"));

        final CertificateTemplate template = certificateRequest.getTemplateId() != null
                ? certificateTemplateRepository.findById(certificateRequest.getTemplateId()).orElse(null)
                : null;

        List<Certificate> savedCertificates = new ArrayList<>();

        certificateRequest.getCertificates().forEach(item -> {
            Certificate certificate = CertificateMapper.toEntity(
                    item, magazine, certificateRequest.getVolume(), certificateRequest.getNumber(),
                    certificateRequest.getType(), template);

            // Salva a entidade para gerar o ID e garantir consistência
            Certificate saved = certificateRepository.save(certificate);

            // Decodifica e salva o PDF no disco
            if (item.getPdfBase64() != null && !item.getPdfBase64().isBlank()) {
                savePdfToDisk(saved.getValidationCode(), item.getPdfBase64());
            }

            savedCertificates.add(saved);
        });

        // Dispara envio de e-mail em background buscando os arquivos do disco
        certificateEmailService.sendBatch(savedCertificates);
    }

    private void savePdfToDisk(UUID validationCode, String base64Pdf) {
        try {
            byte[] pdfBytes = Base64.getDecoder().decode(base64Pdf);
            Path filePath = Paths.get(storagePath, validationCode.toString() + ".pdf");
            Files.write(filePath, pdfBytes);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao salvar o PDF do certificado no disco", e);
        }
    }

    @Transactional
    public void resendEmail(String code) {
        Certificate certificate = certificateRepository.findByValidationCode(UUID.fromString(code))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Certificado não encontrado"));
        certificateEmailService.resend(certificate);
    }

    @Transactional(readOnly = true)
    public CertificateResponse validateCertificate(String code) {
        return certificateRepository.findByValidationCode(UUID.fromString(code))
                .map(certificateMapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Certificado não encontrado"));
    }

    @Transactional(readOnly = true)
    public byte[] downloadCertificate(String code) {
        try {
            Path filePath = Paths.get(storagePath, code + ".pdf");
            if (!Files.exists(filePath)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Arquivo do certificado não encontrado no disco");
            }
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao ler o arquivo do certificado");
        }
    }
}
