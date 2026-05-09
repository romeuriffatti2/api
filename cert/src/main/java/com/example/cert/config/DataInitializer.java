package com.example.cert.config;

import com.example.cert.domain.CertificateTemplate;
import com.example.cert.repository.CertificateTemplateRepository;
import com.example.cert.service.UserService;
import com.example.cert.domain.Usuario;
import com.example.cert.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Component
@AllArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

  private final CertificateTemplateRepository templateRepository;
  private final UserRepository userRepository;
  private final UserService userService;

  private static final String BLANK_PDF = "\"data:application/pdf;base64,JVBERi0xLjcKJYGBgYEKCjUgMCBvYmoKPDwKL0ZpbHRlciAvRmxhdGVEZWNvZGUKL1R5cGUgL09ialN0bQovTiA0Ci9GaXJzdCAyMAovTGVuZ3RoIDI2OAo+PgpzdHJlYW0KeJzVkktLxDAQx+/5FHPUy2aSpmkipbD2cRFhWTy5eAjbsBRks/QB+u2dNKviQTxL+JPH/Cav/whAkKAUZFAYUJBnEsqS8af3iwe+cyc/Mf4w9BMcKIqwhxfG67CcZxCsqtg3W7vZvYYTS0kgIvxJ7MbQL0c/Qtm1XYdYIKJWJI0oG+prkiVJmlNMGhqTCnUVrRUZYralWJeki5QT4yubX/Nb6onVkWkSq0yaf50bz2rTHvKv+9iK8cfQN272cNPcSZQac7RCKCvx+Za+Y/RuDv/3cev9h3D+9YU/fI72RpNHH2tgdZnv/RSW8Ui2E1fF//L94O7DG1UNUjNKbIyF3OYbaaiCCPkAmeOPJQplbmRzdHJlYW0KZW5kb2JqCgo2IDAgb2JqCjw8Ci9TaXplIDcKL1Jvb3QgMiAwIFIKL0luZm8gMyAwIFIKL0ZpbHRlciAvRmxhdGVEZWNvZGUKL1R5cGUgL1hSZWYKL0xlbmd0aCAzNAovVyBbIDEgMiAyIF0KL0luZGV4IFsgMCA3IF0KPj4Kc3RyZWFtCnicFcQxDgAgCASwHsbdN/txCB2K7nLZstV24pF8BkOhArYKZW5kc3RyZWFtCmVuZG9iagoKc3RhcnR4cmVmCjM4NgolJUVPRg==\"";

  @Override
  @Transactional
  public void run(String... args) {
    log.info("Iniciando Verificação de Dados...");

    // Garante que os templates padrão existem
    initializeDefaultTemplates();

    // Onboarding para o usuário admin de teste
    userRepository.findByEmail("romeu.riffatti.2@gmail.com").ifPresent(this::ensureUserHasTemplates);
  }

  private void ensureUserHasTemplates(Usuario user) {
    log.info("Verificando templates para o usuário de teste: {}", user.getEmail());
    
    List<CertificateTemplate> userTemplates = templateRepository.findByOwner(user);
    if (userTemplates.isEmpty()) {
      log.info("Clonando templates iniciais para o usuário: {}", user.getEmail());
      userService.registerUserTemplates(user);
    } else {
      log.info("Sincronizando schemas corrigidos (Base64) para o usuário: {}", user.getEmail());
      for (CertificateTemplate ut : userTemplates) {
        templateRepository.findBySystemDefaultTrue().stream()
            .filter(st -> st.getType().equals(ut.getType()))
            .findFirst()
            .ifPresent(st -> {
              ut.setJsonSchema(st.getJsonSchema());
              templateRepository.save(ut);
              log.info("[SYNC] Template '{}' do usuário atualizado com novo Base64.", ut.getType());
            });
      }
    }
    log.info("Processamento do usuário {} concluído.", user.getEmail());
  }

  private void initializeDefaultTemplates() {
    log.info("Inicializando/Atualizando templates padrão do sistema...");

    saveOrUpdateTemplate("Certificado de Participação", "participacao", buildParticipacaoSchema());
    saveOrUpdateTemplate("Certificado de Publicação", "publicacao", buildPublicacaoSchema());
    saveOrUpdateTemplate("Declaração Ad Hoc (Parecerista)", "parecerista", buildPareceristSchema());
    saveOrUpdateTemplate("Declaração de Corpo Editorial", "corpo-editorial", buildEditorialSchema());
    saveOrUpdateTemplate("Declaração de Dossiê Temático", "dossie", buildDossieSchema());
    saveOrUpdateTemplate("Declaração de Aceite de Artigo", "aceite", buildAceiteSchema());

    log.info("Processamento de templates padrão concluído.");
  }

  private void saveOrUpdateTemplate(String name, String type, String jsonSchema) {
    templateRepository.findBySystemDefaultTrue().stream()
        .filter(t -> t.getType().equals(type))
        .findFirst()
        .ifPresentOrElse(
            existing -> {
              existing.setName(name);
              existing.setJsonSchema(jsonSchema);
              templateRepository.save(existing);
              log.info("Template padrão atualizado: {}", type);
            },
            () -> {
              CertificateTemplate t = CertificateTemplate.builder()
                  .name(name)
                  .type(type)
                  .jsonSchema(jsonSchema)
                  .systemDefault(true)
                  .active(true)
                  .owner(null)
                  .sourceTemplateId(null)
                  .build();
              templateRepository.save(t);
              log.info("Novo template padrão criado: {}", type);
            });
  }

  // ─── Helpers para blocos de schema reutilizáveis ─────────────────────────

  private String validationField(double x, double y) {
    return String.format(Locale.US, """
        {
          "name": "validationInfo",
          "type": "text",
          "content": "Acesse a plataforma para verificar\\nse este certificado é válido.\\nCódigo: {{validationCode}}",
          "position": {"x": %s, "y": %s},
          "width": 50, "height": 15,
          "fontSize": 7, "fontColor": "#aaaaaa",
          "alignment": "right", "verticalAlignment": "top",
          "lineHeight": 1.3
        }""", x, y);
  }

  /** Logo da revista (imagem) */
  private String logoField(double x, double y) {
    return String.format(Locale.US, """
        {
          "name": "logo",
          "type": "image",
          "content": "",
          "position": {"x": %s, "y": %s},
          "width": 50, "height": 25
        }""", x, y);
  }

  /** Área de assinatura */
  private String signatureField(double x, double y, String content) {
    return String.format(Locale.US, """
        {
          "name": "signature",
          "type": "text",
          "content": "%s",
          "position": {"x": %s, "y": %s},
          "width": 150, "height": 30,
          "fontSize": 10, "fontColor": "#333333",
          "alignment": "center", "verticalAlignment": "top",
          "lineHeight": 1.4
        }""", content.replace("\"", "\\\""), x, y);
  }

  /** Rodapé esquerdo (cidade e data) */
  private String footerLeftField(double x, double y) {
    return String.format(Locale.US, """
        {
          "name": "footerLeft",
          "type": "text",
          "content": "Florianópolis, {{date}}",
          "position": {"x": %s, "y": %s},
          "width": 100, "height": 10,
          "fontSize": 9, "fontColor": "#777777",
          "alignment": "left"
        }""", x, y);
  }

  /** Rodapé direito (emissor) */
  private String footerRightField(double x, double y) {
    return String.format(Locale.US, """
        {
          "name": "footerRight",
          "type": "text",
          "content": "Responsável: Centro de Estudos Interdisciplinares LTDA\\nCNPJ 30.704.187/0001-75",
          "position": {"x": %s, "y": %s},
          "width": 100, "height": 15,
          "fontSize": 8, "fontColor": "#777777",
          "alignment": "right", "lineHeight": 1.4
        }""", x, y);
  }

  // ─── CERTIFICADO DE PARTICIPAÇÃO ─────────────────────────────────────────

  private String buildParticipacaoSchema() {
    return String.format(Locale.US,
        """
            {
              "basePdf": %s,
              "schemas": [[
                %s,
                {
                  "name": "title",
                  "type": "text",
                  "content": "Certificado",
                  "position": {"x": 20, "y": 60},
                  "width": 257, "height": 30,
                  "fontSize": 32, "fontColor": "#000000",
                  "alignment": "center", "characterSpacing": 3
                },
                %s,
                {
                  "name": "body",
                  "type": "text",
                  "content": "Certificamos, para os devidos fins, que {{name}} participou e concluiu com sucesso as atividades relacionadas à revista {{magazineName}}, registrada sob o ISSN {{issn}}.",
                  "position": {"x": 25, "y": 100},
                  "width": 247, "height": 40,
                  "fontSize": 16, "fontColor": "#333333",
                  "alignment": "center", "lineHeight": 1.6
                },
                %s,
                %s,
                %s
              ]]
            }""",
        BLANK_PDF,
        validationField(240, 10),
        logoField(123.5, 30),
        signatureField(73.5, 145,
            "____________________________________\\nMe. Ewerton da Silva Ferreira\\nEditor Chefe da {{magazineName}}\\nISSN {{issn}} | {{email}}"),
        footerLeftField(20, 190),
        footerRightField(177, 185));
  }

  // ─── CERTIFICADO DE PUBLICAÇÃO ───────────────────────────────────────────

  private String buildPublicacaoSchema() {
    return String.format(Locale.US,
        """
            {
              "basePdf": %s,
              "schemas": [[
                %s,
                {
                  "name": "title",
                  "type": "text",
                  "content": "Certificado de Publicação",
                  "position": {"x": 20, "y": 60},
                  "width": 257, "height": 20,
                  "fontSize": 20, "fontColor": "#000000",
                  "alignment": "center", "characterSpacing": 2
                },
                %s,
                {
                  "name": "body",
                  "type": "text",
                  "content": "A {{magazineName}}, ISSN {{issn}} (Online), vinculada ao Centro de Estudos Interdisciplinares (CEEINTER), certifica, para os devidos fins, que o artigo intitulado {{articleTitle}}, de autoria de {{name}}, foi publicado no Volume {{volume}}, Número {{number}}, referente ao ano de {{year}}, sob o DOI {{doi}}.\\n\\nO artigo encontra-se disponível para acesso aberto no site oficial da revista: {{accessLink}}.\\n\\nA publicação foi aprovada após processo de avaliação por pares ad hoc.",
                  "position": {"x": 20, "y": 95},
                  "width": 257, "height": 60,
                  "fontSize": 14, "fontColor": "#333333",
                  "alignment": "justify", "lineHeight": 1.6
                },
                %s,
                %s,
                %s
              ]]
            }""",
        BLANK_PDF,
        validationField(240, 10),
        logoField(123.5, 25),
        signatureField(73.5, 160,
            "____________________________________\\nMe. Ewerton da Silva Ferreira\\nEditor Chefe da {{magazineName}}\\nISSN {{issn}} | {{email}}"),
        footerLeftField(20, 190),
        footerRightField(177, 185));
  }

  // ─── DECLARAÇÃO AD HOC (PARECERISTA) ─────────────────────────────────────

  private String buildPareceristSchema() {
    return String.format(Locale.US,
        """
            {
              "basePdf": %s,
              "schemas": [[
                %s,
                {
                  "name": "title",
                  "type": "text",
                  "content": "Declaração Ad Hoc",
                  "position": {"x": 20, "y": 60},
                  "width": 257, "height": 20,
                  "fontSize": 20, "fontColor": "#000000",
                  "alignment": "center", "characterSpacing": 2
                },
                %s,
                {
                  "name": "body",
                  "type": "text",
                  "content": "A revista {{magazineName}} declara para os devidos fins, que {{name}} desempenhou a função de parecerista AD HOC, no volume {{volume}}, número {{number}}, ID da avaliação {{evaluationId}}, de {{year}}.",
                  "position": {"x": 20, "y": 100},
                  "width": 257, "height": 50,
                  "fontSize": 14, "fontColor": "#333333",
                  "alignment": "center", "lineHeight": 1.6
                },
                %s,
                %s,
                %s
              ]]
            }""",
        BLANK_PDF,
        validationField(240, 10),
        logoField(123.5, 25),
        signatureField(73.5, 155,
            "____________________________________\\nMe. Ewerton da Silva Ferreira\\nEditor Chefe da {{magazineName}}\\nISSN {{issn}} | {{email}}"),
        footerLeftField(20, 190),
        footerRightField(177, 185));
  }

  // ─── DECLARAÇÃO DE CORPO EDITORIAL ───────────────────────────────────────

  private String buildEditorialSchema() {
    return String.format(Locale.US,
        """
            {
              "basePdf": %s,
              "schemas": [[
                %s,
                {
                  "name": "title",
                  "type": "text",
                  "content": "Declaração de Membro do Corpo Editorial",
                  "position": {"x": 20, "y": 60},
                  "width": 257, "height": 20,
                  "fontSize": 20, "fontColor": "#000000",
                  "alignment": "center", "characterSpacing": 2
                },
                %s,
                {
                  "name": "body",
                  "type": "text",
                  "content": "A {{magazineName}}, ISSN {{issn}} (Online), vinculada ao Centro de Estudos Interdisciplinares (CEEINTER), declara, para os devidos fins, que {{name}}, portador(a) do CPF {{cpf}}, atua como membro voluntário do Corpo Editorial do periódico desde {{startDate}}, a {{endDate}}, contribuindo de forma significativa para o fortalecimento das atividades editoriais e científicas da revista.",
                  "position": {"x": 20, "y": 100},
                  "width": 257, "height": 60,
                  "fontSize": 14, "fontColor": "#333333",
                  "alignment": "justify", "lineHeight": 1.6
                },
                %s,
                %s,
                %s
              ]]
            }""",
        BLANK_PDF,
        validationField(240, 10),
        logoField(123.5, 25),
        signatureField(73.5, 155,
            "____________________________________\\nMe. Ewerton da Silva Ferreira\\nEditor Chefe da {{magazineName}}\\nISSN {{issn}} | {{email}}"),
        footerLeftField(20, 190),
        footerRightField(177, 185));
  }

  // ─── DECLARAÇÃO DE DOSSIÊ TEMÁTICO ───────────────────────────────────────

  private String buildDossieSchema() {
    return String.format(Locale.US,
        """
            {
              "basePdf": %s,
              "schemas": [[
                %s,
                {
                  "name": "title",
                  "type": "text",
                  "content": "Declaração de Organização de Dossiê Temático",
                  "position": {"x": 20, "y": 60},
                  "width": 257, "height": 20,
                  "fontSize": 18, "fontColor": "#000000",
                  "alignment": "center", "characterSpacing": 2
                },
                %s,
                {
                  "name": "body",
                  "type": "text",
                  "content": "A {{magazineName}}, ISSN {{issn}} (Online), vinculada ao Centro de Estudos Interdisciplinares (CEEINTER), declara, para os devidos fins, que {{name}}, portador(a) do CPF {{cpf}}, atuou como organizador(a) do Dossiê Temático {{dossieTitle}}, publicado no Volume {{volume}}, Número {{number}}, referente ao ano de {{year}}.\\n\\nSua atuação compreendeu a coordenação do processo de submissão, avaliação e seleção de artigos, em conformidade com as diretrizes editoriais da revista.",
                  "position": {"x": 20, "y": 95},
                  "width": 257, "height": 60,
                  "fontSize": 14, "fontColor": "#333333",
                  "alignment": "justify", "lineHeight": 1.6
                },
                %s,
                %s,
                %s
              ]]
            }""",
        BLANK_PDF,
        validationField(240, 10),
        logoField(123.5, 25),
        signatureField(73.5, 160,
            "____________________________________\\nMe. Ewerton da Silva Ferreira\\nEditor Chefe da {{magazineName}}\\nISSN {{issn}} | {{email}}"),
        footerLeftField(20, 190),
        footerRightField(177, 185));
  }

  // ─── DECLARAÇÃO DE ACEITE DE ARTIGO ──────────────────────────────────────

  private String buildAceiteSchema() {
    return String.format(Locale.US,
        """
            {
              "basePdf": %s,
              "schemas": [[
                %s,
                {
                  "name": "title",
                  "type": "text",
                  "content": "Declaração de Aceite de Artigo",
                  "position": {"x": 20, "y": 60},
                  "width": 257, "height": 20,
                  "fontSize": 20, "fontColor": "#000000",
                  "alignment": "center", "characterSpacing": 2
                },
                %s,
                {
                  "name": "body",
                  "type": "text",
                  "content": "Prezados(as) {{name}},\\n\\nA {{magazineName}}, ISSN {{issn}} (Online), vinculada ao Centro de Estudos Interdisciplinares (CEEINTER), tem a satisfação de informar que o artigo intitulado {{articleTitle}}, submetido para avaliação, foi ACEITO para publicação, com previsão de lançamento no Volume {{volume}}, Número {{number}}, referente ao mês/ano de {{publishMonthYear}}.\\n\\nO trabalho foi aprovado após avaliação por pares ad hoc.",
                  "position": {"x": 20, "y": 95},
                  "width": 257, "height": 60,
                  "fontSize": 14, "fontColor": "#333333",
                  "alignment": "justify", "lineHeight": 1.6
                },
                %s,
                %s,
                %s
              ]]
            }""",
        BLANK_PDF,
        validationField(240, 10),
        logoField(123.5, 25),
        signatureField(73.5, 160,
            "____________________________________\\nMe. Ewerton da Silva Ferreira\\nEditor Chefe da {{magazineName}}\\nISSN {{issn}} | {{email}}"),
        footerLeftField(20, 190),
        footerRightField(177, 185));
  }
}
