package com.example.cert.service;

import com.example.cert.Response.CertificateResponse;
import lombok.RequiredArgsConstructor;
import jakarta.annotation.PostConstruct;
import com.example.cert.domain.Certificate;
import com.example.cert.domain.CertificateTemplate;
import com.example.cert.domain.Magazine;
import com.example.cert.domain.Person;
import com.example.cert.mapper.CertificateMapper;
import com.example.cert.repository.CertificateRepository;
import com.example.cert.repository.CertificateTemplateRepository;
import com.example.cert.repository.MagazineRepository;
import com.example.cert.repository.PersonRepository;
import com.example.cert.request.CertificateItemRequest;
import com.example.cert.request.CertificateRequest;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class CertificateService {

    private final CertificateRepository certificateRepository;
    private final CertificateMapper certificateMapper;
    private final MagazineRepository magazineRepository;
    private final CertificateTemplateRepository certificateTemplateRepository;
    private final CertificateEmailService certificateEmailService;
    private final PersonRepository personRepository;

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
            // 1. Resolve ou cria a Person (upsert) — garante que person_id nunca seja nulo
            Person person = resolveOrCreatePerson(item);

            // 2. Cria o Certificate com a Person já garantida
            Certificate certificate = CertificateMapper.toEntity(
                    item, person, magazine,
                    certificateRequest.getVolume(), certificateRequest.getNumber(),
                    certificateRequest.getType(), template);

            // 3. Salva para gerar o ID e garantir consistência
            Certificate saved = certificateRepository.save(certificate);

            // 4. Decodifica e salva o PDF no disco
            if (item.getPdfBase64() != null && !item.getPdfBase64().isBlank()) {
                savePdfToDisk(saved.getValidationCode(), item.getPdfBase64());
            }

            savedCertificates.add(saved);
        });

        // Dispara envio de e-mail em background buscando os arquivos do disco
        certificateEmailService.sendBatch(savedCertificates);
    }

    /**
     * Resolve a Person correta para o certificado, garantindo que person_id
     * seja sempre preenchido independente do modo de emissão.
     *
     * - Modo "Pesquisar Pessoas": personId já conhecido → busca por ID (confiável)
     * - Modo "Adicionar Manualmente": upsert por CPF → cria Person se não existir
     */
    private Person resolveOrCreatePerson(CertificateItemRequest item) {

        // Modo "Pesquisar Pessoas": personId enviado pelo frontend
        if (item.getPersonId() != null) {
            return personRepository.findById(item.getPersonId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Pessoa não encontrada com id=" + item.getPersonId()));
        }

        // Modo "Adicionar Manualmente": upsert por CPF (chave natural única)
        String rawCpf = item.getCpf();
        if (rawCpf == null || rawCpf.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "CPF é obrigatório para emissão de certificado no modo manual.");
        }

        // Normaliza CPF removendo pontuação (ex: "123.456.789-09" → "12345678909")
        String normalizedCpf = rawCpf.replaceAll("[^\\d]", "");

        return personRepository.findByCpf(normalizedCpf).orElseGet(() -> {
            log.info("Criando nova Person via fluxo manual: name='{}', cpf='{}'",
                    item.getName(), normalizedCpf);
            Person newPerson = Person.builder()
                    .name(item.getName())
                    .email(item.getEmail())
                    .cpf(normalizedCpf)
                    .build();
            return personRepository.save(newPerson);
        });
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

    /**
     * Busca todos os certificados vinculados ao e-mail informado e os reenvia.
     * Sempre retorna normalmente — nunca revela se o e-mail está cadastrado
     * na plataforma (conformidade LGPD / segurança por obscuridade).
     */
    @Transactional(readOnly = true)
    public void sendCertificatesByEmail(String email) {
        List<Certificate> certificates = certificateRepository.findByPersonEmail(email.trim().toLowerCase());
        if (!certificates.isEmpty()) {
            log.info("Disparando reenvio de {} certificado(s) para o e-mail: {}", certificates.size(), email);
            certificateEmailService.sendBatch(certificates);
        } else {
            log.info("Nenhum certificado encontrado para o e-mail: {} (resposta omitida ao cliente)", email);
        }
    }
}
