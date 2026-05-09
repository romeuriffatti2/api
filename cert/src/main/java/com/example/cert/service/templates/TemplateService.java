package com.example.cert.service.templates;

import com.example.cert.Exceptions.BusinessException;
import com.example.cert.Response.TemplateResponse;
import com.example.cert.domain.CertificateTemplate;
import com.example.cert.domain.Usuario;
import com.example.cert.repository.CertificateTemplateRepository;
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

  @Transactional
  public void cloneTemplatesForUser(Usuario newUser) {
    List<CertificateTemplate> systemDefaults = templateRepository.findBySystemDefaultTrue();

    systemDefaults.forEach(template -> {
      boolean alreadyHas = templateRepository.findByOwner(newUser).stream()
          .anyMatch(t -> template.getId().equals(t.getSourceTemplateId()));

      if (!alreadyHas) {
        CertificateTemplate userCopy = CertificateTemplate.builder()
            .name(template.getName())
            .type(template.getType())
            .jsonSchema(template.getJsonSchema())
            .systemDefault(false)
            .active(true)
            .sourceTemplateId(template.getId())
            .owner(newUser)
            .build();
        templateRepository.save(userCopy);
      }
    });
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
                  .owner(null)
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

  @Transactional(readOnly = true)
  public List<TemplateResponse> listMyTemplates(Usuario owner) {
    return templateRepository.findByOwner(owner).stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public TemplateResponse getMyTemplateById(Long id, Usuario owner) {
    CertificateTemplate template = templateRepository.findByIdAndOwner(id, owner)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Template não encontrado ou sem permissão"));
    return toResponse(template);
  }

  @Transactional(readOnly = true)
  public TemplateResponse getMyTemplateByType(String type, Usuario owner) {
    CertificateTemplate template = templateRepository.findByOwner(owner).stream()
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
  public TemplateResponse update(Long id, SaveTemplateRequest req, Usuario owner) {
    CertificateTemplate template = templateRepository.findByIdAndOwner(id, owner)
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
  public TemplateResponse create(SaveTemplateRequest req, Usuario owner) {
    CertificateTemplate template = CertificateTemplate.builder()
        .name(req.getName())
        .issuerName(req.getIssuerName())
        .type("custom")
        .jsonSchema(req.getJsonSchema())
        .systemDefault(false)
        .active(true)
        .owner(owner)
        .sourceTemplateId(null)
        .build();
    return toResponse(templateRepository.save(template));
  }

  @Transactional
  public TemplateResponse cloneMyTemplate(Long id, Usuario owner) {
    CertificateTemplate original = templateRepository.findByIdAndOwner(id, owner)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Template não encontrado ou sem permissão"));

    CertificateTemplate copy = CertificateTemplate.builder()
        .name(original.getName() + " (cópia)")
        .issuerName(original.getIssuerName())
        .type(original.getType())
        .jsonSchema(original.getJsonSchema())
        .systemDefault(false)
        .active(true)
        .owner(owner)
        .sourceTemplateId(original.getSourceTemplateId())
        .build();
    return toResponse(templateRepository.save(copy));
  }

  @Transactional
  public TemplateResponse resetToDefault(Long id, Usuario owner) {
    CertificateTemplate userTemplate = templateRepository.findByIdAndOwner(id, owner)
        .orElseThrow(() -> new BusinessException("Template não encontrado ou sem permissão"));

    if (userTemplate.getSourceTemplateId() == null) {
      throw new BusinessException("Este template não possui um padrão de origem para reset.");
    }

    CertificateTemplate sourceTemplate = templateRepository
        .findById(userTemplate.getSourceTemplateId())
        .orElseThrow(() -> new BusinessException("Template padrão de origem não encontrado."));

    userTemplate.setJsonSchema(sourceTemplate.getJsonSchema());
    userTemplate.setName(sourceTemplate.getName());

    return toResponse(templateRepository.save(userTemplate));
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
