package com.example.cert.service;

import com.example.cert.Response.CertificateResponse;
import lombok.RequiredArgsConstructor;
import jakarta.annotation.PostConstruct;
import com.example.cert.domain.Certificate;
import com.example.cert.domain.CertificateTemplate;
import com.example.cert.domain.EmailStatus;
import com.example.cert.domain.Magazine;
import com.example.cert.domain.Person;
import com.example.cert.domain.PersonEmail;
import com.example.cert.mapper.CertificateMapper;
import com.example.cert.repository.CertificateRepository;
import com.example.cert.repository.CertificateTemplateRepository;
import com.example.cert.repository.MagazineRepository;
import com.example.cert.repository.PersonEmailRepository;
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
    private final PersonEmailRepository personEmailRepository;

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
     * - Modo "Adicionar Manualmente": verifica conflitos por CPF e e-mail entre pessoas
     *   ativas. Se já existir, orienta o operador a usar o modo "Pesquisar Pessoas".
     *   Se não existir, cria nova Person e registra o e-mail no histórico.
     */
    private Person resolveOrCreatePerson(CertificateItemRequest item) {

        // Modo "Pesquisar Pessoas": personId enviado pelo frontend
        if (item.getPersonId() != null) {
            return personRepository.findById(item.getPersonId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Pessoa não encontrada com id=" + item.getPersonId()));
        }

        // Modo "Adicionar Manualmente": CPF obrigatório
        String rawCpf = item.getCpf();
        if (rawCpf == null || rawCpf.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "CPF é obrigatório para emissão de certificado no modo manual.");
        }

        String normalizedCpf = rawCpf.replaceAll("[^\\d]", "");

        // Bloqueia se CPF já pertence a pessoa ativa — deve usar modo Pesquisar Pessoas
        if (personRepository.findByCpfAndDeletedFalse(normalizedCpf).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "CPF já cadastrado no sistema. Adicione esta pessoa pelo modo 'Pesquisar Pessoas'.");
        }

        // Bloqueia se e-mail já pertence a pessoa ativa
        if (item.getEmail() != null && !item.getEmail().isBlank()) {
            String normalizedEmail = item.getEmail().trim().toLowerCase();
            if (personRepository.findByEmailAndDeletedFalse(normalizedEmail).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "E-mail já cadastrado no sistema. Adicione esta pessoa pelo modo 'Pesquisar Pessoas'.");
            }
        }

        // Nenhum conflito → cria nova Person
        return personRepository.findByCpfAndDeletedFalse(normalizedCpf).orElseGet(() -> {
            log.info("Criando nova Person via fluxo manual: name='{}', cpf='{}'",
                    item.getName(), normalizedCpf);
            Person newPerson = Person.builder()
                    .name(item.getName())
                    .email(item.getEmail())
                    .cpf(normalizedCpf)
                    .build();
            Person saved = personRepository.save(newPerson);

            // Registra o e-mail em person_email para rastreamento histórico
            if (saved.getEmail() != null && !saved.getEmail().isBlank()) {
                registerEmailHistory(saved, saved.getEmail());
            }

            return saved;
        });
    }


    /**
     * Registra um e-mail na tabela person_email se ainda não existir.
     * Chamado ao criar uma Person para garantir que o e-mail esteja no histórico
     * e seja resolvível na funcionalidade "Receber por E-mail".
     */
    private void registerEmailHistory(Person person, String email) {
        String normalized = email.trim().toLowerCase();
        if (!personEmailRepository.existsByEmail(normalized)) {
            PersonEmail personEmail = PersonEmail.builder()
                    .person(person)
                    .email(normalized)
                    .status(EmailStatus.ACTIVE)
                    .createdAt(java.time.LocalDateTime.now())
                    .build();
            personEmailRepository.save(personEmail);
        }
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
     * <p>
     * A busca é feita na tabela {@code person_email} (histórico), permitindo que
     * e-mails antigos (inativos) também resolvam a Person correta. Os certificados
     * são sempre enviados para o e-mail <strong>ativo atual</strong> da Person.
     * <p>
     * Sempre retorna normalmente — nunca revela se o e-mail está cadastrado
     * na plataforma (conformidade LGPD / segurança por obscuridade).
     */
    @Transactional(readOnly = true)
    public void sendCertificatesByEmail(String email) {
        String normalizedEmail = email.trim().toLowerCase();

        personEmailRepository.findByEmail(normalizedEmail).ifPresentOrElse(
                personEmail -> {
                    Person person = personEmail.getPerson();
                    List<Certificate> certificates = certificateRepository.findByPerson(person);

                    if (!certificates.isEmpty()) {
                        log.info("Disparando reenvio de {} certificado(s) para a Person id={} (e-mail ativo: {})",
                                certificates.size(), person.getId(), person.getEmail());
                        // Envia sempre para o e-mail ATIVO atual, mesmo que a busca tenha sido por e-mail antigo
                        certificateEmailService.sendBatchToAddress(certificates, person.getEmail());
                    } else {
                        log.info("Person id={} encontrada pelo histórico mas sem certificados (e-mail buscado: {})",
                                person.getId(), normalizedEmail);
                    }
                },
                () -> log.info("Nenhuma person encontrada para o e-mail: {} (resposta omitida ao cliente)", normalizedEmail)
        );
    }
}
