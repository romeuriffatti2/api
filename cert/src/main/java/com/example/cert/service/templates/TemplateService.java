package com.example.cert.service.templates;

import com.example.cert.Exceptions.BusinessException;
import com.example.cert.Response.TemplateResponse;
import com.example.cert.domain.CertificateTemplate;
import com.example.cert.domain.Magazine;
import com.example.cert.domain.Usuario;
import com.example.cert.repository.CertificateTemplateRepository;
import com.example.cert.repository.MagazineRepository;
import com.example.cert.request.SaveTemplateRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class TemplateService {

  private final CertificateTemplateRepository templateRepository;
  private final MagazineRepository magazineRepository;

  /**
   * Clona todos os templates padrões do sistema para uma nova revista
   */
  @Transactional
  public void cloneTemplatesForMagazine(Magazine newMagazine) {
    List<CertificateTemplate> systemDefaults = templateRepository.findBySystemDefaultTrue();

    systemDefaults.forEach(template -> {
      CertificateTemplate magazineCopy = CertificateTemplate.builder()
          .name(template.getName())
          .type(template.getType())
          .jsonSchema(template.getJsonSchema())
          .systemDefault(false)
          .active(true)
          .sourceTemplateId(template.getId())
          .magazine(newMagazine)
          .build();
      templateRepository.save(magazineCopy);
    });
    log.info("Templates base clonados para a revista id={}", newMagazine.getId());
  }

  public void saveOrUpdateTemplate(String name, String type, String jsonSchema) {
    templateRepository.findBySystemDefaultTrue().stream()
        .filter(t -> t.getType().equals(type))
        .findFirst()
        .ifPresentOrElse(
            existing -> {
              existing.setName(name);
              existing.setJsonSchema(jsonSchema);
              templateRepository.save(existing);
            },
            () -> {
              CertificateTemplate t = CertificateTemplate.builder()
                  .name(name).type(type)
                  .jsonSchema(jsonSchema)
                  .systemDefault(true)
                  .active(true)
                  .magazine(null)
                  .sourceTemplateId(null)
                  .build();
              templateRepository.save(t);
            });
  }

  private TemplateResponse toResponse(CertificateTemplate t) {
    return TemplateResponse.builder()
        .id(t.getId())
        .name(t.getName())
        .issuerName(t.getIssuerName())
        .type(t.getType())
        .systemDefault(t.isSystemDefault())
        .sourceTemplateId(t.getSourceTemplateId())
        .jsonSchema(t.getJsonSchema())
        .createdAt(t.getCreatedAt())
        .updatedAt(t.getUpdatedAt())
        .build();
  }

  private Magazine resolveAndValidateMagazine(Long magazineId, Usuario owner) {
    return magazineRepository.findByIdAndOwner(magazineId, owner)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Revista não encontrada ou sem permissão de acesso"));
  }

  @Transactional(readOnly = true)
  public List<TemplateResponse> listTemplatesByMagazine(Long magazineId, Usuario owner) {
    Magazine magazine = resolveAndValidateMagazine(magazineId, owner);
    return templateRepository.findByMagazine(magazine).stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public TemplateResponse getTemplateByIdAndMagazine(Long id, Long magazineId, Usuario owner) {
    Magazine magazine = resolveAndValidateMagazine(magazineId, owner);
    CertificateTemplate template = templateRepository.findByIdAndMagazine(id, magazine)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Template não encontrado ou sem permissão"));
    return toResponse(template);
  }

  @Transactional(readOnly = true)
  public TemplateResponse getTemplateByTypeAndMagazine(String type, Long magazineId, Usuario owner) {
    Magazine magazine = resolveAndValidateMagazine(magazineId, owner);
    CertificateTemplate template = templateRepository.findByMagazine(magazine).stream()
        .filter(t -> type.equals(t.getType()))
        .findFirst()
        .orElseGet(() -> templateRepository.findBySystemDefaultTrue().stream()
            .filter(t -> type.equals(t.getType()))
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Template não encontrado para o tipo: " + type)));
    return toResponse(template);
  }

  @Transactional
  public TemplateResponse update(Long id, Long magazineId, SaveTemplateRequest req, Usuario owner) {
    Magazine magazine = resolveAndValidateMagazine(magazineId, owner);
    CertificateTemplate template = templateRepository.findByIdAndMagazine(id, magazine)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Template não encontrado ou sem permissão"));

    if (req.getName() != null && !req.getName().isBlank()) {
      template.setName(req.getName());
    }
    if (req.getIssuerName() != null) {
      template.setIssuerName(req.getIssuerName());
    }
    if (req.getJsonSchema() != null && !req.getJsonSchema().isBlank()) {
      template.setJsonSchema(req.getJsonSchema());
    }
    return toResponse(templateRepository.save(template));
  }

  @Transactional
  public TemplateResponse create(Long magazineId, SaveTemplateRequest req, Usuario owner) {
    Magazine magazine = resolveAndValidateMagazine(magazineId, owner);
    CertificateTemplate template = CertificateTemplate.builder()
        .name(req.getName())
        .issuerName(req.getIssuerName())
        .type("custom")
        .jsonSchema(req.getJsonSchema())
        .systemDefault(false)
        .active(true)
        .magazine(magazine)
        .sourceTemplateId(null)
        .build();
    return toResponse(templateRepository.save(template));
  }

  @Transactional
  public TemplateResponse cloneTemplate(Long id, Long magazineId, Usuario owner) {
    Magazine magazine = resolveAndValidateMagazine(magazineId, owner);
    CertificateTemplate original = templateRepository.findByIdAndMagazine(id, magazine)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Template não encontrado ou sem permissão"));

    CertificateTemplate copy = CertificateTemplate.builder()
        .name(original.getName() + " (cópia)")
        .issuerName(original.getIssuerName())
        .type(original.getType())
        .jsonSchema(original.getJsonSchema())
        .systemDefault(false)
        .active(true)
        .magazine(magazine)
        .sourceTemplateId(original.getSourceTemplateId())
        .build();
    return toResponse(templateRepository.save(copy));
  }

  @Transactional
  public TemplateResponse resetToDefault(Long id, Long magazineId, Usuario owner) {
    Magazine magazine = resolveAndValidateMagazine(magazineId, owner);
    CertificateTemplate magazineTemplate = templateRepository.findByIdAndMagazine(id, magazine)
        .orElseThrow(() -> new BusinessException("Template não encontrado ou sem permissão"));

    if (magazineTemplate.getSourceTemplateId() == null) {
      throw new BusinessException("Este template não possui um padrão de origem para reset.");
    }

    CertificateTemplate sourceTemplate = templateRepository
        .findById(magazineTemplate.getSourceTemplateId())
        .orElseThrow(() -> new BusinessException("Template padrão de origem não encontrado."));

    magazineTemplate.setJsonSchema(sourceTemplate.getJsonSchema());
    magazineTemplate.setName(sourceTemplate.getName());

    return toResponse(templateRepository.save(magazineTemplate));
  }

  @Transactional(readOnly = true)
  public List<TemplateResponse> listSystemTemplates() {
    return templateRepository.findBySystemDefaultTrue()
        .stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional
  public TemplateResponse updateSystemTemplate(Long id, SaveTemplateRequest req) {
    CertificateTemplate template = templateRepository.findById(id)
        .filter(CertificateTemplate::isSystemDefault)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Template padrão do sistema não encontrado"));

    if (req.getName() != null && !req.getName().isBlank()) {
      template.setName(req.getName());
    }
    if (req.getJsonSchema() != null && !req.getJsonSchema().isBlank()) {
      template.setJsonSchema(req.getJsonSchema());
    }
    return toResponse(templateRepository.save(template));
  }
}
