package com.example.cert.config;

import com.example.cert.domain.CertificateTemplate;
import com.example.cert.repository.CertificateTemplateRepository;
import com.example.cert.service.UserService;
import com.example.cert.domain.Usuario;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Inicializa os templates padrão do sistema no banco de dados se ainda não existirem.
 * Os schemas PDFME foram construídos com base nos templates HTML (Thymeleaf) existentes,
 * mantendo a mesma estrutura de conteúdo (título, corpo do texto, assinatura, rodapé).
 *
 * Layout: A4 paisagem = 841.89 x 595.28 pontos.
 * Campos uses variáveis {{name}}, {{magazineName}}, {{issn}}, {{volume}}, {{number}},
 * {{date}}, {{year}}, {{validationCode}}, {{articleTitle}}, {{doi}}, {{accessLink}},
 * {{evaluationId}}, {{cpf}}, {{startDate}}, {{endDate}}, {{dossieTitle}}, {{publishMonthYear}}.
 */
@Component
@AllArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final CertificateTemplateRepository templateRepository;
    private final com.example.cert.repository.UserRepository userRepository;
    private final UserService userService;

    private static final String BLANK_PDF = "{ \\\"width\\\": 297, \\\"height\\\": 210 }";

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
        if (templateRepository.findByOwner(user).isEmpty()) {
            log.info("Clonando templates para o usuário existente: {}", user.getEmail());
            userService.registerUserTemplates(user);
        }
    }

    private void initializeDefaultTemplates() {
        log.info("Inicializando/Atualizando templates padrão do sistema...");

        saveOrUpdateTemplate("Certificado de Participação",      "participacao",     buildParticipacaoSchema());
        saveOrUpdateTemplate("Certificado de Publicação",        "publicacao",       buildPublicacaoSchema());
        saveOrUpdateTemplate("Declaração Ad Hoc (Parecerista)", "parecerista",      buildPareceristSchema());
        saveOrUpdateTemplate("Declaração de Corpo Editorial",   "corpo-editorial",  buildEditorialSchema());
        saveOrUpdateTemplate("Declaração de Dossiê Temático",   "dossie",           buildDossieSchema());
        saveOrUpdateTemplate("Declaração de Aceite de Artigo",  "aceite",           buildAceiteSchema());

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
                    }
                );
    }

    // ─── Helpers para blocos de schema reutilizáveis ─────────────────────────

    /** Cabeçalho de validação (canto superior direito) */
    private String validationField(double x, double y) {
        return """
            {
              "name": "validationInfo",
              "type": "text",
              "content": "Acesse a plataforma para verificar\\nse este certificado é válido.\\nCódigo: {{validationCode}}",
              "position": {"x": %s, "y": %s},
              "width": 50, "height": 15,
              "fontSize": 7, "fontColor": "#aaaaaa",
              "alignment": "right", "verticalAlignment": "top",
              "lineHeight": 1.3
            }""".formatted(x, y);
    }

    /** Logo da revista (imagem) */
    private String logoField(double x, double y) {
        return """
            {
              "name": "logo",
              "type": "image",
              "content": "",
              "position": {"x": %s, "y": %s},
              "width": 50, "height": 25
            }""".formatted(x, y);
    }

    /** Área de assinatura */
    private String signatureField(double x, double y, String content) {
        return """
            {
              "name": "signature",
              "type": "text",
              "content": "%s",
              "position": {"x": %s, "y": %s},
              "width": 150, "height": 30,
              "fontSize": 10, "fontColor": "#333333",
              "alignment": "center", "verticalAlignment": "top",
              "lineHeight": 1.4
            }""".formatted(content.replace("\"", "\\\""), x, y);
    }

    /** Rodapé esquerdo (cidade e data) */
    private String footerLeftField(double x, double y) {
        return """
            {
              "name": "footerLeft",
              "type": "text",
              "content": "Florianópolis, {{date}}",
              "position": {"x": %s, "y": %s},
              "width": 100, "height": 10,
              "fontSize": 9, "fontColor": "#777777",
              "alignment": "left"
            }""".formatted(x, y);
    }

    /** Rodapé direito (emissor) */
    private String footerRightField(double x, double y) {
        return """
            {
              "name": "footerRight",
              "type": "text",
              "content": "Responsável: Centro de Estudos Interdisciplinares LTDA\\nCNPJ 30.704.187/0001-75",
              "position": {"x": %s, "y": %s},
              "width": 100, "height": 15,
              "fontSize": 8, "fontColor": "#777777",
              "alignment": "right", "lineHeight": 1.4
            }""".formatted(x, y);
    }

    // ─── CERTIFICADO DE PARTICIPAÇÃO ─────────────────────────────────────────

    private String buildParticipacaoSchema() {
        return """
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
              "fontSize": 32, "fontColor": "#000000", "fontStyle": "bold",
              "alignment": "center", "textTransform": "uppercase", "characterSpacing": 3
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
        }""".formatted(
                BLANK_PDF,
                validationField(240, 10),
                logoField(123.5, 30),
                signatureField(73.5, 145,
                        "____________________________________\\nMe. Ewerton da Silva Ferreira\\nEditor Chefe da {{magazineName}}\\nISSN {{issn}} | {{email}}"),
                footerLeftField(20, 190),
                footerRightField(177, 185)
        );
    }

    // ─── CERTIFICADO DE PUBLICAÇÃO ───────────────────────────────────────────

    private String buildPublicacaoSchema() {
        return """
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
              "fontSize": 20, "fontColor": "#000000", "fontStyle": "bold",
              "alignment": "center", "textTransform": "uppercase", "characterSpacing": 2
            },
            %s,
            {
              "name": "body",
              "type": "text",
              "content": "A {{magazineName}}, ISSN {{issn}} (Online), vinculada ao Centro de Estudos Interdisciplinares (CEEINTER), certifica, para os devidos fins, que o artigo intitulado \\"{{articleTitle}}\\", de autoria de {{name}}, foi publicado no Volume {{volume}}, Número {{number}}, referente ao ano de {{year}}, sob o DOI {{doi}}.\\n\\nO artigo encontra-se disponível para acesso aberto no site oficial da revista: {{accessLink}}.\\n\\nA publicação foi aprovada após processo de avaliação por pares ad hoc.",
              "position": {"x": 20, "y": 95},
              "width": 257, "height": 60,
              "fontSize": 14, "fontColor": "#333333",
              "alignment": "justified", "lineHeight": 1.6
            },
            %s,
            %s,
            %s
          ]]
        }""".formatted(
                BLANK_PDF,
                validationField(240, 10),
                logoField(123.5, 25),
                signatureField(73.5, 160,
                        "____________________________________\\nMe. Ewerton da Silva Ferreira\\nEditor Chefe da {{magazineName}}\\nISSN {{issn}} | {{email}}"),
                footerLeftField(20, 190),
                footerRightField(177, 185)
        );
    }

    // ─── DECLARAÇÃO AD HOC (PARECERISTA) ─────────────────────────────────────

    private String buildPareceristSchema() {
        return """
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
              "fontSize": 20, "fontColor": "#000000", "fontStyle": "bold",
              "alignment": "center", "textTransform": "uppercase", "characterSpacing": 2
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
        }""".formatted(
                BLANK_PDF,
                validationField(240, 10),
                logoField(123.5, 25),
                signatureField(73.5, 155,
                        "____________________________________\\nMe. Ewerton da Silva Ferreira\\nEditor Chefe da {{magazineName}}\\nISSN {{issn}} | {{email}}"),
                footerLeftField(20, 190),
                footerRightField(177, 185)
        );
    }

    // ─── DECLARAÇÃO DE CORPO EDITORIAL ───────────────────────────────────────

    private String buildEditorialSchema() {
        return """
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
              "fontSize": 20, "fontColor": "#000000", "fontStyle": "bold",
              "alignment": "center", "textTransform": "uppercase", "characterSpacing": 2
            },
            %s,
            {
              "name": "body",
              "type": "text",
              "content": "A {{magazineName}}, ISSN {{issn}} (Online), vinculada ao Centro de Estudos Interdisciplinares (CEEINTER), declara, para os devidos fins, que {{name}}, portador(a) do CPF {{cpf}}, atua como membro voluntário do Corpo Editorial do periódico desde {{startDate}}, a {{endDate}}, contribuindo de forma significativa para o fortalecimento das atividades editoriais e científicas da revista.",
              "position": {"x": 20, "y": 100},
              "width": 257, "height": 60,
              "fontSize": 14, "fontColor": "#333333",
              "alignment": "justified", "lineHeight": 1.6
            },
            %s,
            %s,
            %s
          ]]
        }""".formatted(
                BLANK_PDF,
                validationField(240, 10),
                logoField(123.5, 25),
                signatureField(73.5, 155,
                        "____________________________________\\nMe. Ewerton da Silva Ferreira\\nEditor Chefe da {{magazineName}}\\nISSN {{issn}} | {{email}}"),
                footerLeftField(20, 190),
                footerRightField(177, 185)
        );
    }

    // ─── DECLARAÇÃO DE DOSSIÊ TEMÁTICO ───────────────────────────────────────

    private String buildDossieSchema() {
        return """
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
              "fontSize": 18, "fontColor": "#000000", "fontStyle": "bold",
              "alignment": "center", "textTransform": "uppercase", "characterSpacing": 2
            },
            %s,
            {
              "name": "body",
              "type": "text",
              "content": "A {{magazineName}}, ISSN {{issn}} (Online), vinculada ao Centro de Estudos Interdisciplinares (CEEINTER), declara, para os devidos fins, que {{name}}, portador(a) do CPF {{cpf}}, atuou como organizador(a) do Dossiê Temático intitulado \\"{{dossieTitle}}\\", publicado no Volume {{volume}}, Número {{number}}, referente ao ano de {{year}}.\\n\\nSua atuação compreendeu a coordenação do processo de submissão, avaliação e seleção de artigos, em conformidade com as diretrizes editoriais da revista.",
              "position": {"x": 20, "y": 95},
              "width": 257, "height": 60,
              "fontSize": 14, "fontColor": "#333333",
              "alignment": "justified", "lineHeight": 1.6
            },
            %s,
            %s,
            %s
          ]]
        }""".formatted(
                BLANK_PDF,
                validationField(240, 10),
                logoField(123.5, 25),
                signatureField(73.5, 160,
                        "____________________________________\\nMe. Ewerton da Silva Ferreira\\nEditor Chefe da {{magazineName}}\\nISSN {{issn}} | {{email}}"),
                footerLeftField(20, 190),
                footerRightField(177, 185)
        );
    }

    // ─── DECLARAÇÃO DE ACEITE DE ARTIGO ──────────────────────────────────────

    private String buildAceiteSchema() {
        return """
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
              "fontSize": 20, "fontColor": "#000000", "fontStyle": "bold",
              "alignment": "center", "textTransform": "uppercase", "characterSpacing": 2
            },
            %s,
            {
              "name": "body",
              "type": "text",
              "content": "Prezados(as) {{name}},\\n\\nA {{magazineName}}, ISSN {{issn}} (Online), vinculada ao Centro de Estudos Interdisciplinares (CEEINTER), tem a satisfação de informar que o artigo intitulado \\"{{articleTitle}}\\", submetido para avaliação, foi ACEITO para publicação, com previsão de lançamento no Volume {{volume}}, Número {{number}}, referente ao mês/ano de {{publishMonthYear}}.\\n\\nO trabalho foi aprovado após avaliação por pares ad hoc.",
              "position": {"x": 20, "y": 95},
              "width": 257, "height": 60,
              "fontSize": 14, "fontColor": "#333333",
              "alignment": "justified", "lineHeight": 1.6
            },
            %s,
            %s,
            %s
          ]]
        }""".formatted(
                BLANK_PDF,
                validationField(240, 10),
                logoField(123.5, 25),
                signatureField(73.5, 160,
                        "____________________________________\\nMe. Ewerton da Silva Ferreira\\nEditor Chefe da {{magazineName}}\\nISSN {{issn}} | {{email}}"),
                footerLeftField(20, 190),
                footerRightField(177, 185)
        );
    }
}
