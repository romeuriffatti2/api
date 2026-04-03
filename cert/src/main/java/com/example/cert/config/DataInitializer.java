package com.example.cert.config;

import com.example.cert.domain.CertificateTemplate;
import com.example.cert.repository.CertificateTemplateRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

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

    @Override
    public void run(String... args) {
        List<CertificateTemplate> existing = templateRepository.findBySystemDefaultTrue();
        if (!existing.isEmpty()) {
            log.info("Templates padrão do sistema já inicializados ({} templates).", existing.size());
            return;
        }

        log.info("Inicializando templates padrão do sistema...");

        saveTemplate("Certificado de Participação",      "participacao",     buildParticipacaoSchema());
        saveTemplate("Certificado de Publicação",        "publicacao",       buildPublicacaoSchema());
        saveTemplate("Declaração Ad Hoc (Parecerista)", "parecerista",      buildPareceristSchema());
        saveTemplate("Declaração de Corpo Editorial",   "corpo-editorial",  buildEditorialSchema());
        saveTemplate("Declaração de Dossiê Temático",   "dossie",           buildDossieSchema());
        saveTemplate("Declaração de Aceite de Artigo",  "aceite",           buildAceiteSchema());

        log.info("6 templates padrão criados com sucesso.");
    }

    private void saveTemplate(String name, String type, String jsonSchema) {
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
              "width": 140, "height": 20,
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
              "width": 200, "height": 35,
              "fontSize": 10, "fontColor": "#333333",
              "alignment": "center", "verticalAlignment": "top",
              "lineHeight": 1.5
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
              "width": 120, "height": 10,
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
              "width": 180, "height": 15,
              "fontSize": 9, "fontColor": "#777777",
              "alignment": "right", "lineHeight": 1.4
            }""".formatted(x, y);
    }

    // ─── CERTIFICADO DE PARTICIPAÇÃO ─────────────────────────────────────────

    private String buildParticipacaoSchema() {
        return """
        {
          "basePdf": "__BLANK_PDF__",
          "schemas": [[
            %s,
            {
              "name": "title",
              "type": "text",
              "content": "Certificado",
              "position": {"x": 170.945, "y": 60},
              "width": 500, "height": 30,
              "fontSize": 32, "fontColor": "#000000", "fontStyle": "bold",
              "alignment": "center", "textTransform": "uppercase", "characterSpacing": 3
            },
            %s,
            {
              "name": "body",
              "type": "text",
              "content": "Certificamos, para os devidos fins, que {{name}} participou e concluiu com sucesso as atividades relacionadas à revista {{magazineName}}, registrada sob o ISSN {{issn}}.",
              "position": {"x": 70.945, "y": 140},
              "width": 700, "height": 50,
              "fontSize": 16, "fontColor": "#333333",
              "alignment": "center", "lineHeight": 1.6
            },
            %s,
            %s,
            %s
          ]]
        }""".formatted(
                validationField(680, 20),
                logoField(378, 105),
                signatureField(320, 225,
                        "____________________________________\\nMe. Ewerton da Silva Ferreira\\nEditor Chefe da {{magazineName}}\\nISSN {{issn}} | {{email}}"),
                footerLeftField(55, 290),
                footerRightField(505, 285)
        );
    }

    // ─── CERTIFICADO DE PUBLICAÇÃO ───────────────────────────────────────────

    private String buildPublicacaoSchema() {
        return """
        {
          "basePdf": "__BLANK_PDF__",
          "schemas": [[
            %s,
            {
              "name": "title",
              "type": "text",
              "content": "Certificado de Publicação",
              "position": {"x": 170.945, "y": 55},
              "width": 500, "height": 20,
              "fontSize": 20, "fontColor": "#000000", "fontStyle": "bold",
              "alignment": "center", "textTransform": "uppercase", "characterSpacing": 2
            },
            %s,
            {
              "name": "body",
              "type": "text",
              "content": "A {{magazineName}}, ISSN {{issn}} (Online), vinculada ao Centro de Estudos Interdisciplinares (CEEINTER), certifica, para os devidos fins, que o artigo intitulado \\"{{articleTitle}}\\", de autoria de {{name}}, foi publicado no Volume {{volume}}, Número {{number}}, referente ao ano de {{year}}, sob o DOI {{doi}}.\\n\\nO artigo encontra-se disponível para acesso aberto no site oficial da revista: {{accessLink}}.\\n\\nA publicação foi aprovada após processo de avaliação por pares ad hoc.",
              "position": {"x": 55, "y": 125},
              "width": 730, "height": 100,
              "fontSize": 14, "fontColor": "#333333",
              "alignment": "justified", "lineHeight": 1.6
            },
            %s,
            %s,
            %s
          ]]
        }""".formatted(
                validationField(680, 20),
                logoField(378, 82),
                signatureField(320, 240,
                        "____________________________________\\nMe. Ewerton da Silva Ferreira\\nEditor Chefe da {{magazineName}}\\nISSN {{issn}} | {{email}}"),
                footerLeftField(55, 290),
                footerRightField(505, 285)
        );
    }

    // ─── DECLARAÇÃO AD HOC (PARECERISTA) ─────────────────────────────────────

    private String buildPareceristSchema() {
        return """
        {
          "basePdf": "__BLANK_PDF__",
          "schemas": [[
            %s,
            {
              "name": "title",
              "type": "text",
              "content": "Declaração Ad Hoc",
              "position": {"x": 170.945, "y": 55},
              "width": 500, "height": 20,
              "fontSize": 20, "fontColor": "#000000", "fontStyle": "bold",
              "alignment": "center", "textTransform": "uppercase", "characterSpacing": 2
            },
            %s,
            {
              "name": "body",
              "type": "text",
              "content": "A revista {{magazineName}} declara para os devidos fins, que {{name}} desempenhou a função de parecerista AD HOC, no volume {{volume}}, número {{number}}, ID da avaliação {{evaluationId}}, de {{year}}.",
              "position": {"x": 55, "y": 130},
              "width": 730, "height": 60,
              "fontSize": 14, "fontColor": "#333333",
              "alignment": "justified", "lineHeight": 1.6
            },
            %s,
            %s,
            %s
          ]]
        }""".formatted(
                validationField(680, 20),
                logoField(378, 82),
                signatureField(320, 235,
                        "____________________________________\\nMe. Ewerton da Silva Ferreira\\nEditor Chefe da {{magazineName}}\\nISSN {{issn}} | {{email}}"),
                footerLeftField(55, 290),
                footerRightField(505, 285)
        );
    }

    // ─── DECLARAÇÃO DE CORPO EDITORIAL ───────────────────────────────────────

    private String buildEditorialSchema() {
        return """
        {
          "basePdf": "__BLANK_PDF__",
          "schemas": [[
            %s,
            {
              "name": "title",
              "type": "text",
              "content": "Declaração de Membro do Corpo Editorial",
              "position": {"x": 170.945, "y": 55},
              "width": 500, "height": 20,
              "fontSize": 20, "fontColor": "#000000", "fontStyle": "bold",
              "alignment": "center", "textTransform": "uppercase", "characterSpacing": 2
            },
            %s,
            {
              "name": "body",
              "type": "text",
              "content": "A {{magazineName}}, ISSN {{issn}} (Online), vinculada ao Centro de Estudos Interdisciplinares (CEEINTER), declara, para os devidos fins, que {{name}}, portador(a) do CPF {{cpf}}, atua como membro voluntário do Corpo Editorial do periódico desde {{startDate}}, a {{endDate}}, contribuindo de forma significativa para o fortalecimento das atividades editoriais e científicas da revista.",
              "position": {"x": 55, "y": 125},
              "width": 730, "height": 80,
              "fontSize": 14, "fontColor": "#333333",
              "alignment": "justified", "lineHeight": 1.6
            },
            %s,
            %s,
            %s
          ]]
        }""".formatted(
                validationField(680, 20),
                logoField(378, 82),
                signatureField(320, 235,
                        "____________________________________\\nMe. Ewerton da Silva Ferreira\\nEditor Chefe da {{magazineName}}\\nISSN {{issn}} | {{email}}"),
                footerLeftField(55, 290),
                footerRightField(505, 285)
        );
    }

    // ─── DECLARAÇÃO DE DOSSIÊ TEMÁTICO ───────────────────────────────────────

    private String buildDossieSchema() {
        return """
        {
          "basePdf": "__BLANK_PDF__",
          "schemas": [[
            %s,
            {
              "name": "title",
              "type": "text",
              "content": "Declaração de Organização de Dossiê Temático",
              "position": {"x": 170.945, "y": 55},
              "width": 500, "height": 20,
              "fontSize": 18, "fontColor": "#000000", "fontStyle": "bold",
              "alignment": "center", "textTransform": "uppercase", "characterSpacing": 2
            },
            %s,
            {
              "name": "body",
              "type": "text",
              "content": "A {{magazineName}}, ISSN {{issn}} (Online), vinculada ao Centro de Estudos Interdisciplinares (CEEINTER), declara, para os devidos fins, que {{name}}, portador(a) do CPF {{cpf}}, atuou como organizador(a) do Dossiê Temático intitulado \\"{{dossieTitle}}\\", publicado no Volume {{volume}}, Número {{number}}, referente ao ano de {{year}}.\\n\\nSua atuação compreendeu a coordenação do processo de submissão, avaliação e seleção de artigos, em conformidade com as diretrizes editoriais da revista.",
              "position": {"x": 55, "y": 125},
              "width": 730, "height": 90,
              "fontSize": 14, "fontColor": "#333333",
              "alignment": "justified", "lineHeight": 1.6
            },
            %s,
            %s,
            %s
          ]]
        }""".formatted(
                validationField(680, 20),
                logoField(378, 82),
                signatureField(320, 240,
                        "____________________________________\\nMe. Ewerton da Silva Ferreira\\nEditor Chefe da {{magazineName}}\\nISSN {{issn}} | {{email}}"),
                footerLeftField(55, 290),
                footerRightField(505, 285)
        );
    }

    // ─── DECLARAÇÃO DE ACEITE DE ARTIGO ──────────────────────────────────────

    private String buildAceiteSchema() {
        return """
        {
          "basePdf": "__BLANK_PDF__",
          "schemas": [[
            %s,
            {
              "name": "title",
              "type": "text",
              "content": "Declaração de Aceite de Artigo",
              "position": {"x": 170.945, "y": 55},
              "width": 500, "height": 20,
              "fontSize": 20, "fontColor": "#000000", "fontStyle": "bold",
              "alignment": "center", "textTransform": "uppercase", "characterSpacing": 2
            },
            %s,
            {
              "name": "body",
              "type": "text",
              "content": "Prezados(as) {{name}},\\n\\nA {{magazineName}}, ISSN {{issn}} (Online), vinculada ao Centro de Estudos Interdisciplinares (CEEINTER), tem a satisfação de informar que o artigo intitulado \\"{{articleTitle}}\\", submetido para avaliação, foi ACEITO para publicação, com previsão de lançamento no Volume {{volume}}, Número {{number}}, referente ao mês/ano de {{publishMonthYear}}.\\n\\nO trabalho foi aprovado após avaliação por pares ad hoc.",
              "position": {"x": 55, "y": 125},
              "width": 730, "height": 100,
              "fontSize": 14, "fontColor": "#333333",
              "alignment": "justified", "lineHeight": 1.6
            },
            %s,
            %s,
            %s
          ]]
        }""".formatted(
                validationField(680, 20),
                logoField(378, 82),
                signatureField(320, 240,
                        "____________________________________\\nMe. Ewerton da Silva Ferreira\\nEditor Chefe da {{magazineName}}\\nISSN {{issn}} | {{email}}"),
                footerLeftField(55, 290),
                footerRightField(505, 285)
        );
    }
}
