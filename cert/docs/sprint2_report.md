# Sprint 2 — Relatório de Execução: Serviço de E-mail e Assets

**Data:** 01/04/2026  
**Status:** ✅ Concluído

---

## Resumo

Sprint 2 executada com sucesso. O pipeline de envio de certificados por e-mail foi implantado de forma **não-bloqueante** — o usuário recebe o download do PDF imediatamente, e os e-mails são disparados em background via `@Async`. O sistema de armazenamento de assets (imagens, logos, fundos de templates) também foi criado, preparando o terreno para o editor PDFME da Sprint 3.

---

## Alterações Realizadas

### API — Spring Boot

#### ✅ `pom.xml` — Nova Dependência

Adicionada a dependência do módulo de e-mail do Spring Boot:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

---

#### ✅ `application.yaml` — Novas Configurações

Dois novos blocos adicionados:

**Gmail SMTP (variáveis de ambiente via `.env`):**
```yaml
spring:
  mail:
    host: ${MAIL_HOST:smtp.gmail.com}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME:}
    password: ${MAIL_PASSWORD:}
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true
```

**Diretório de upload:**
```yaml
app:
  upload:
    dir: ${UPLOAD_DIR:uploads}
```

> [!IMPORTANT]
> Para o e-mail funcionar em produção, adicione ao seu `.env`:
> ```env
> MAIL_HOST=smtp.gmail.com
> MAIL_PORT=587
> MAIL_USERNAME=seuemail@gmail.com
> MAIL_PASSWORD=xxxx_xxxx_xxxx_xxxx   # App Password do Google (não é sua senha pessoal)
> ```
> Para gerar a App Password: **myaccount.google.com → Segurança → Verificação em duas etapas → Senhas de app**

---

#### ✅ `CertApplication.java` — `@EnableAsync` adicionado

```java
@SpringBootApplication
@EnableAsync   // ← NOVO: habilita @Async globalmente
public class CertApplication { ... }
```

Necessário para que o `CertificateEmailService` possa disparar threads em background sem bloquear a resposta HTTP.

---

#### ✅ `CertificateEmailService.java` — NOVO (`service/`)

Serviço responsável pelo ciclo de vida do envio de e-mails.

| Método | Descrição |
|---|---|
| `sendBatch(List<Certificate>, byte[])` | `@Async` — itera o lote e envia para cada destinatário |
| `send(Certificate, String, byte[])` | Envia um e-mail com o PDF em anexo e persiste o novo status |
| `resolveRecipient(Certificate)` | Prioriza `recipientEmail` (desnormalizado), cai para `person.email` |
| `buildEmailBody(Certificate)` | Gera o HTML do corpo do e-mail com nome e código de validação |

**Fluxo de status resultante:**
```
Certificado gerado → status: GENERATED
       ↓ (async)
E-mail enviado com sucesso → status: EMAIL_SENT
E-mail com falha          → status: EMAIL_FAILED  (pode ser reprocessado)
```

---

#### ✅ `FileStorageService.java` — NOVO (`service/`)

Serviço de armazenamento de arquivos locais. Usado para salvar imagens de fundo e logos dos templates PDFME.

| Método | Descrição |
|---|---|
| `store(MultipartFile)` | Salva o arquivo com nome UUID, retorna `/uploads/{uuid}.{ext}` |
| `delete(String filename)` | Remove o arquivo do disco com proteção contra path traversal |
| `validate(MultipartFile)` | Verifica tamanho (max 5 MB) e tipo (jpeg, png, webp, gif) |

---

#### ✅ `WebMvcConfig.java` — NOVO (`config/`)

Expõe o diretório de uploads como endpoint HTTP estático.

```java
registry.addResourceHandler("/uploads/**")
        .addResourceLocations("file:" + absolutePath + "/");
```

Com isso, qualquer imagem salva pelo `FileStorageService` fica acessível via:
```
GET http://localhost:8080/uploads/{uuid}.png
```

> O `SecurityConfig.java` já tinha `.requestMatchers("/uploads/**").permitAll()` desde a Sprint 1, portanto nenhuma alteração adicional no filtro de segurança foi necessária.

---

#### ✅ `CertificateService.java` — Integração do e-mail

O método `create()` foi atualizado para disparar o envio de e-mail após a geração do PDF:

```java
byte[] pdfBytes = generatePdfService.generatePdf(savedCertificates);

// Async: não bloqueia o download do usuário
certificateEmailService.sendBatch(savedCertificates, pdfBytes);

return pdfBytes;
```

---

## Arquivos Criados / Modificados

| Arquivo | Tipo | Descrição |
|---|---|---|
| `pom.xml` | MODIFICADO | `spring-boot-starter-mail` adicionado |
| `application.yaml` | MODIFICADO | Config SMTP Gmail + diretório de upload |
| `CertApplication.java` | MODIFICADO | `@EnableAsync` adicionado |
| `CertificateService.java` | MODIFICADO | Integração com `CertificateEmailService` |
| `CertificateEmailService.java` | **NOVO** | Envio de PDF por e-mail com atualização de status |
| `FileStorageService.java` | **NOVO** | Armazenamento de imagens com validação e UUID |
| `WebMvcConfig.java` | **NOVO** | Exposição de `/uploads/**` como recurso estático |

---

## Nenhuma Mudança no Banco de Dados

> [!NOTE]
> A Sprint 2 não requer nenhuma migration de banco de dados.
> A coluna `status` e `recipient_email` em `certificate` já foram adicionadas na Sprint 1.
> O Hibernate gerenciará automaticamente qualquer alteração residual se `ddl-auto=update`.

---

## Pendências para Próximos Sprints

| Item | Sprint |
|---|---|
| Endpoint `POST /api/my/assets/image` — controller que usa `FileStorageService` | Sprint 3 |
| Template de e-mail Thymeleaf com logo e visual da plataforma | Sprint 3 |
| Endpoint de reprocessamento de e-mails com `status = EMAIL_FAILED` | Sprint 4+ |
| `UserService.createClient()` — clonagem automática de templates no onboarding | Sprint 3 |
| Editor PDFME + endpoints `/api/my/templates` completos | Sprint 3 |
