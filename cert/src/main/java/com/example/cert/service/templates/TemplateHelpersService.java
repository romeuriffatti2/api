package com.example.cert.service.templates;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Slf4j
@Service
public class TemplateHelpersService {

  public static final String BLANK_PDF = "\"data:application/pdf;base64,JVBERi0xLjcKJeLjz9MKMSAwIG9iago8PAovVHlwZSAvQ2F0YWxvZwovUGFnZXMgMiAwIFIKPj4KZW5kb2JqCjIgMCBvYmoKPDwKL1R5cGUgL1BhZ2VzCi9LaWRzIFszIDAgUl0KL0NvdW50IDEKPj4KZW5kb2JqCjMgMCBvYmoKPDwKL1R5cGUgL1BhZ2UKL1BhcmVudCAyIDAgUgovTWVkaWFCb3ggWzAgMCA4NDEuODkgNTk1LjI4XQovUmVzb3VyY2VzIDw8CiAgL1Byb2NTZXQgWy9QREYgL1RleHQgL0ltYWdlQiAvSW1hZ2VDIC9JbWFnZUldCj4+Ci9Db250ZW50cyA0IDAgUgo+PgplbmRvYmoKNCAwIG9iago8PAovTGVuZ3RoIDAKPj4Kc3RyZWFtCmVuZHN0cmVhbQplbmRvYmoKeHJlZgowIDUKMDAwMDAwMDAwMCA2NTUzNSBmIAowMDAwMDAwMDE1IDAwMDAwIG4gCjAwMDAwMDAwNjggMDAwMDAgbiAKMDAwMDAwMDEyNSAwMDAwMCBuIAowMDAwMDAwMjcxIDAwMDAwIG4gCnRyYWlsZXIKPDwKL1NpemUgNQovUm9vdCAxIDAgUgo+PgpzdGFydHhyZWYKMzIwCiUlRU9GCg==\"";

  // ─── Helpers para blocos de schema reutilizáveis ─────────────────────────

  public String validationField(double x, double y) {
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

  public String logoField(double x, double y) {
    return String.format(Locale.US, """
        {
          "name": "logo",
          "type": "image",
          "content": "",
          "position": {"x": %s, "y": %s},
          "width": 50, "height": 25
        }""", x, y);
  }

  public String signatureField(double x, double y, String content) {
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

  public String footerLeftField(double x, double y) {
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

  public String footerRightField(double x, double y) {
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

  // ─── Builders de Schema Completo ──────────────────────────────────────────

  public String buildParticipacaoSchema() {
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
            "____________________________________\\n{{responsavelTecnico}}\\nEditor Chefe da {{magazineName}}\\nISSN {{issn}} | {{email}}"),
        footerLeftField(20, 190),
        footerRightField(177, 185));
  }

  public String buildPublicacaoSchema() {
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
            "____________________________________\\n{{responsavelTecnico}}\\nEditor Chefe da {{magazineName}}\\nISSN {{issn}} | {{email}}"),
        footerLeftField(20, 190),
        footerRightField(177, 185));
  }

  public String buildPareceristSchema() {
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
            "____________________________________\\n{{responsavelTecnico}}\\nEditor Chefe da {{magazineName}}\\nISSN {{issn}} | {{email}}"),
        footerLeftField(20, 190),
        footerRightField(177, 185));
  }

  public String buildEditorialSchema() {
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
            "____________________________________\\n{{responsavelTecnico}}\\nEditor Chefe da {{magazineName}}\\nISSN {{issn}} | {{email}}"),
        footerLeftField(20, 190),
        footerRightField(177, 185));
  }

  public String buildDossieSchema() {
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
            "____________________________________\\n{{responsavelTecnico}}\\nEditor Chefe da {{magazineName}}\\nISSN {{issn}} | {{email}}"),
        footerLeftField(20, 190),
        footerRightField(177, 185));
  }

  public String buildAceiteSchema() {
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
                  "content": "Prezados(as) {{name}},\\n\\nA {{magazineName}}, ISSN {{issn}} (Online), vinculada ao Centro de Estudos Interdisciplinares (CEEINTER), tem a satisfação de informar que o artigo intitulado {{articleTitle}}, submetido para avaliação, foi ACEITO para publicação, com previsão de lançamento no Volume {{volume}}, Número {{number}}, referente ao mês/ano de {{publishMonth}}/{{publishYear}}.\\n\\nO trabalho foi aprovado após avaliação por pares ad hoc.",
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
            "____________________________________\\n{{responsavelTecnico}}\\nEditor Chefe da {{magazineName}}\\nISSN {{issn}} | {{email}}"),
        footerLeftField(20, 190),
        footerRightField(177, 185));
  }
}
