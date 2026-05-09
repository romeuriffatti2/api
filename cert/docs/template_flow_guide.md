# Guia do Fluxo de Templates de Certificado (JSON Schema)

Este documento descreve detalhadamente como funciona o fluxo de edição e armazenamento dos templates de certificados (JSON Schema) em sua aplicação, abrangendo tanto o Frontend (Angular) quanto o Backend (Spring Boot).

## 1. Visão Geral e Relacionamentos

Você está correto em seu entendimento:
- **Geração Atual:** Quando você gera um certificado, o sistema utiliza a relação entre `Usuario` e `Magazine` (através da tabela intermediária `user_magazine`) para buscar as informações da revista (nome, ISSN, logo, etc.) e preencher o certificado.
- **Templates de Certificado:** O modelo visual (o design do certificado) não está preso à revista. Ele pertence ao `Usuario` (ou é um padrão do sistema). Quando você edita o design do template pelo painel, toda a estrutura visual (textos, imagens, posições, fonte e a imagem de fundo) é exportada pelo editor visual e salva como um **JSON** na coluna `json_schema` da tabela `certificate_template`.

Abaixo detalhamos onde encontrar cada peça desse fluxo.

---

## 2. Onde está o JSON Schema?

O JSON Schema fica armazenado fisicamente no banco de dados relacional (PostgreSQL):
- **Tabela:** `certificate_template`
- **Coluna:** `json_schema` (do tipo `TEXT` ou `LONGVARCHAR`)

Este JSON é gerado e lido pela biblioteca **PDFME**, que é o motor visual utilizado no frontend. O formato básico desse JSON é:
```json
{
  "basePdf": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgA...", // Imagem ou PDF de fundo
  "schemas": [
    {
      "nome_do_campo": {
        "type": "text",
        "position": { "x": 100, "y": 150 },
        "width": 50,
        "height": 10,
        ...
      }
    }
  ]
}
```

---

## 3. Fluxo no Frontend (Angular)

No frontend, a responsabilidade de lidar com esse JSON recai sobre o módulo de edição de templates, localizado na pasta:
`app/src/app/client/templates/`

### A. O Editor (`template-editor.component.ts`)
Este é o coração visual da edição do JSON:
1. **Carregamento (`loadAndInit`):** O componente faz uma requisição para a API buscando o template pelo ID. Ele pega a string `jsonSchema` que veio do banco, faz o `JSON.parse()`, aplica validações de segurança (como tratar imagens de fundo malformadas) e inicializa a classe `Designer` da biblioteca `@pdfme/ui`.
2. **Edição:** O usuário arrasta os campos e muda o fundo no navegador. O PDFME mantém esse estado em memória.
3. **Upload de Fundo (`uploadBackground`):** Quando o usuário faz upload de um novo fundo (JPG ou PNG), o sistema usa a biblioteca `pdf-lib` para converter essa imagem em um PDF base e injeta o resultado em base64 dentro de `schema.basePdf`.
4. **Salvamento (`save`):** Quando o usuário clica em salvar, o componente chama `this.designer.getTemplate()`, que devolve o objeto JSON atualizado. O componente converte isso para string com `JSON.stringify()` e envia para a API via `TemplateService`.

### B. O Serviço (`template.service.ts`)
Arquivo: `app/src/app/services/template.service.ts`
- É a ponte de comunicação com o backend.
- O método `save(id, req)` realiza um **PUT** na rota `/api/my/templates/{id}`, enviando o novo `jsonSchema` em formato string dentro do corpo da requisição.

---

## 4. Fluxo no Backend (Spring Boot)

No backend, o fluxo é responsável por receber o JSON como uma string, validar permissões e salvá-lo no banco de dados.

### A. A Entidade (`CertificateTemplate.java`)
Arquivo: `api/cert/src/main/java/com/example/cert/domain/CertificateTemplate.java`
- É a representação da tabela no banco de dados.
- Possui o campo `private String jsonSchema;` mapeado com `@Column(columnDefinition = "TEXT")`.
- Possui a relação `@ManyToOne` com o `Usuario` dono do template (`owner_id`).

### B. O Controlador (`TemplateController.java`)
Arquivo: `api/cert/src/main/java/com/example/cert/controller/TemplateController.java`
- Expõe os endpoints REST para o frontend (ex: `PUT /api/my/templates/{id}`).
- Recebe o DTO `SaveTemplateRequest` (que contém o `name` e o `jsonSchema`) e repassa a requisição para o Service, informando também qual é o usuário logado no momento.

### C. O Serviço (`TemplateService.java`)
Arquivo: `api/cert/src/main/java/com/example/cert/service/TemplateService.java`
- Contém a regra de negócio.
- No método `update(...)`, ele primeiro verifica se o template pertence ao usuário logado (`templateRepository.findByIdAndOwner()`).
- Em seguida, atualiza a entidade com o novo JSON vindo do frontend: `template.setJsonSchema(req.getJsonSchema());`
- Por fim, salva no banco de dados: `templateRepository.save(template)`.

---

## 5. Resumo da Arquitetura

1. **Frontend (`template-editor.component.ts`)** extrai o modelo visual criado no navegador usando `@pdfme` e o transforma em uma String JSON.
2. **Frontend (`template.service.ts`)** envia esse JSON por HTTP PUT.
3. **Backend (`TemplateController.java` -> `TemplateService.java`)** recebe a string, valida se o usuário é dono daquele template e salva a alteração.
4. O banco de dados (`PostgreSQL`, tabela `certificate_template`) armazena essa string gigantesca no campo `json_schema`.

*(Nota: Atualmente o serviço de geração em lote de PDF `GeneratePdfService` ainda usa Thymeleaf/OpenHTMLToPDF, porém os novos modelos interativos do `certificate_template` estão totalmente preparados para substituir esse fluxo utilizando o PDFME para gerar os arquivos finais.)*
