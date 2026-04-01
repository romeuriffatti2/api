# Guia Arquitetural — Plataforma Institucional de Revista Científica (v3)

> **Atualizado com:** editor de templates WYSIWYG (PDFME), templates pertencendo ao usuário (não à revista), clonagem automática no onboarding, RBAC refinado (Admin vê apenas seus próprios templates + padrões do sistema), reset para template padrão pelo CLIENT.

---

## 1. A Resposta para o Editor — Por que PDFME é a escolha certa

Você quer uma experiência como o **Canva** ou o **Word Online** — o usuário vê o certificado, clica num texto, edita, arrasta elementos, coloca a imagem de fundo. Isso existe, é gratuito e se chama **[PDFME](https://pdfme.com)**.

### Comparativo das opções

| | GrapesJS | PDFME | Fabric.js (canvas custom) |
|---|---|---|---|
| O que parece | Editor de sites (Wix) | **Editor de documentos PDF (Canva/Word)** | Quadro de design livre |
| Curva de implementação | Média | **Baixa** | Alta |
| Output nativo | HTML | **PDF direto** | PNG/Canvas |
| Gratuito (MIT) | ✅ | ✅ | ✅ |
| Funciona no Angular | ✅ | ✅ (vanilla JS) | ✅ |
| Especializado em documentos A4 | ❌ | ✅ | ❌ |
| Arrastar e posicionar campos | ✅ | ✅ | ✅ |

**PDFME é a escolha exata** porque:
1. O usuário posiciona campos arrastando (nome, data, assinatura, logo)
2. Ele edita o texto clicando diretamente no campo
3. O template é um **PDF fixo como fundo + campos variáveis por cima** — perfeito para certificados
4. O mesmo `@pdfme/generator` que você usa no browser gera o PDF final (*ou pode gerar no Java*)

---

## 2. Como Funciona o Novo Pipeline de Templates

```
┌─────────────────────────────────────────────────────────┐
│ DESIGN (1x, feito pelo usuário cliente)                 │
│                                                         │
│  Upload do background (PNG/JPG)                         │
│       ↓ vira basePdf do PDFME                           │
│  PDFME Designer (no browser)                            │
│  → arrasta campos: texto, imagem (logo), QR code        │
│  → configura fontes, tamanhos, posições                 │
│  → clica "Salvar"                                       │
│       ↓                                                 │
│  API salva: { basePdf: "base64...", schemas: [...] }    │
│  no banco em CertificateTemplate.jsonSchema             │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│ GERAÇÃO (toda vez que emite certificados)               │
│                                                         │
│  API busca CertificateTemplate do usuário               │
│  Monta inputs: [{ name: "João", date: "01/04/2026" }]   │
│  @pdfme/generator gera o PDF com os dados reais         │
│       ↓                                                 │
│  PDF pronto → retorna download + envia por e-mail       │
└─────────────────────────────────────────────────────────┘
```

> [!IMPORTANT]
> **Mudança de paradigma no backend:** Com PDFME, o Thymeleaf e o OpenHTMLToPDF **não são mais necessários para os templates customizados**. O `@pdfme/generator` gera o PDF diretamente em TypeScript/JavaScript. Isso significa que a geração de PDF para templates customizados pode acontecer **no próprio browser (client-side)** ou via um pequeno **microserviço Node.js**.
>
> **Os templates padrão do sistema continuam funcionando** com Thymeleaf + OpenHTMLToPDF normalmente, até você migrar completamente.

### Estratégia de transição recomendada

```
Fase 1 (agora): Templates padrão = Thymeleaf + OpenHTMLToPDF (como hoje)
Fase 2 (Sprint 3): Templates customizados via PDFME no browser (client-side generation)
Fase 3 (futuro): Migrar todos os templates para PDFME, aposentar Thymeleaf
```

---

## 3. Modelagem — Templates pertencem ao USUÁRIO

Esta é a correção mais importante: **`CertificateTemplate` tem dono `Usuario`, não `Magazine`**. O usuário cria e mantém seus templates e pode usá-los em qualquer uma de suas revistas.

```
Usuario >---< Magazine          (ManyToMany)
Usuario >---  CertificateTemplate (OneToMany)  ← TEMPLATES SÃO DO USUÁRIO
Magazine >--- Certificate       (OneToMany)
Certificate --- CertificateTemplate (ManyToOne)
```

### 3.1 `CertificateTemplate` — Entidade

```java
@Entity
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CertificateTemplate {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;            // "Meu Certificado de Participação"
    private String type;            // "participacao", "publicacao", "parecerista", "custom"

    private boolean systemDefault;  // true = template padrão do sistema (imutável pelos clientes)
    private boolean active;

    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String jsonSchema;      // JSON do PDFME: { basePdf, schemas }

    // Templates do sistema têm owner = null
    // Templates do usuário têm owner = o usuário que criou/clonou
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = true)
    private Usuario owner;          // ← PERTENCE AO USUÁRIO, não à revista

    // Referência ao template padrão do sistema que originou este template.
    // Permite o CLIENT resetar para o padrão do sistema a qualquer momento.
    // null = template criado do zero pelo usuário (sem origem padrão)
    @Column(name = "source_template_id")
    private Long sourceTemplateId;

    @CreationTimestamp private LocalDateTime createdAt;
    @UpdateTimestamp private LocalDateTime updatedAt;
}
```

### 3.2 Ciclo de Vida — Onboarding e Reset de Templates

**Onboarding:** Quando o Admin cria um novo usuário Cliente, o sistema automaticamente clona todos os templates padrão do sistema. Cada cópia guarda uma referência ao template original via `sourceTemplateId` — isso é o que permite o reset posterior.

```java
// UserService.createClient()
List<CertificateTemplate> systemDefaults = templateRepository.findBySystemDefaultTrue();
systemDefaults.forEach(template -> {
    CertificateTemplate userCopy = CertificateTemplate.builder()
        .name(template.getName())
        .type(template.getType())
        .jsonSchema(template.getJsonSchema()) // cópia exata do JSON
        .systemDefault(false)
        .sourceTemplateId(template.getId())   // ← referência ao original para reset
        .owner(newUser)
        .active(true)
        .build();
    templateRepository.save(userCopy);
});
```

**Reset para o padrão:** O CLIENT pode a qualquer momento resetar um template de volta ao estado do template padrão do sistema de origem. Isso sobrescreve o `jsonSchema` com o do template-fonte, mas **não deleta** — o histórico de quem emitiu com qual template permanece intacto nos registros de `Certificate`.

```java
// TemplateService.resetToDefault(Long templateId, Usuario requester)
CertificateTemplate userTemplate = templateRepository.findByIdAndOwner(templateId, requester)
    .orElseThrow(() -> new BusinessException("Template não encontrado ou sem permissão"));

if (userTemplate.getSourceTemplateId() == null) {
    throw new BusinessException("Este template não possui um padrão de origem para reset");
}

CertificateTemplate sourceTemplate = templateRepository.findById(userTemplate.getSourceTemplateId())
    .orElseThrow(() -> new BusinessException("Template padrão de origem não encontrado"));

userTemplate.setJsonSchema(sourceTemplate.getJsonSchema());
userTemplate.setName(sourceTemplate.getName()); // restaura o nome original também
templateRepository.save(userTemplate);
```

### 3.3 `Usuario` — Revisado

```java
@Enumerated(EnumType.STRING)
private UserRole role; // ADMIN ou CLIENT

@ManyToMany(fetch = FetchType.LAZY)
@JoinTable(
    name = "user_magazine",
    joinColumns = @JoinColumn(name = "user_id"),
    inverseJoinColumns = @JoinColumn(name = "magazine_id")
)
private Set<Magazine> magazines = new HashSet<>();

@OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
private List<CertificateTemplate> templates;
```

### 3.4 `Certificate` — Revisado

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "person_id")
private Person person;

private String recipientEmail;

@Enumerated(EnumType.STRING)
private CertificateStatus status; // GENERATED, EMAIL_SENT, EMAIL_FAILED

// Qual template foi usado para gerar este certificado
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "template_id")
private CertificateTemplate template;
```

### 3.5 Enums

```java
public enum UserRole {
    ADMIN,   // Superusuário da plataforma
    CLIENT   // Responsável por revistas parceiras
}

public enum CertificateStatus {
    GENERATED,      // gerado com sucesso
    EMAIL_SENT,     // e-mail enviado com sucesso
    EMAIL_FAILED    // falha no envio (pode ser reprocessado)
}
```

### 3.6 `NewsPost` — NOVO

```java
@Entity @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class NewsPost {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    @Column(unique = true)
    private String slug;
    @Lob @Column(columnDefinition = "TEXT")
    private String body;
    private String coverImagePath;
    @ManyToOne private Usuario author;
    private boolean published;
    @CreationTimestamp private LocalDateTime createdAt;
    @UpdateTimestamp private LocalDateTime updatedAt;
}
```

### 3.7 `Event` — NOVO

```java
@Entity @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Event {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    @Lob @Column(columnDefinition = "TEXT")
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private String location;
    private String registrationUrl;
    private String coverImagePath;
    private boolean published;
    @CreationTimestamp private LocalDateTime createdAt;
}
```

---

## 4. Regras de Autorização (RBAC)

### Regra geral de templates

> Cada usuário (CLIENT ou ADMIN) é **dono exclusivo** dos seus próprios templates. Ninguém — nem o Admin — pode editar os templates que pertencem a outro usuário. O Admin tem acesso apenas aos seus próprios templates e aos templates padrão do sistema.

| Recurso | PÚBLICO | CLIENT | ADMIN |
|---|---|---|---|
| Ver site institucional | ✅ | ✅ | ✅ |
| Validar / buscar / baixar certificados | ✅ | ✅ | ✅ |
| Ver templates padrão do sistema | ❌ | ✅ (leitura) | ✅ (leitura + edição) |
| Ver e editar **seus próprios** templates | ❌ | ✅ | ✅ |
| Criar template customizado do zero | ❌ | ✅ | ✅ |
| **Resetar template para o padrão do sistema** | ❌ | ✅ | ✅ |
| Ver ou editar templates de **outros usuários** | ❌ | ❌ | ❌ |
| Emitir certificados (suas revistas) | ❌ | ✅ | ✅ |
| Ver histórico (suas revistas) | ❌ | ✅ | ✅ |
| Ver histórico de todas as revistas | ❌ | ❌ | ✅ |
| CRUD Revistas + Usuários | ❌ | ❌ | ✅ |
| CRUD Notícias e Eventos | ❌ | ❌ | ✅ |

---

## 5. Endpoints — Completos e Atualizados

### 5.1 Públicos (sem autenticação)

```
GET  /api/certificates/validate/{uuid}
GET  /api/certificates/download/{uuid}
GET  /api/certificates/search?name=&email=&page=&size=
GET  /api/news?page=&size=
GET  /api/news/{slug}
GET  /api/events?page=&size=
POST /api/auth/login
```

### 5.2 CLIENT ou ADMIN (JWT obrigatório)

```
# Revistas do usuário logado
GET  /api/my/magazines

# Certificados
POST /api/certificates/generate
     Body: { templateId, magazineId, volume, number, recipients: [{name, email, cpf}] }
GET  /api/certificates?magazineId=&page=&size=

# Templates DO USUÁRIO LOGADO
GET    /api/my/templates                      # lista todos os seus templates
GET    /api/my/templates/{id}                 # busca um template (retorna jsonSchema completo)
PUT    /api/my/templates/{id}                 # salva edições do PDFME Designer
POST   /api/my/templates/{id}/clone           # clona um de seus templates
DELETE /api/my/templates/{id}                 # deleta template customizado (se sourceTemplateId != null)
POST   /api/my/templates                      # cria template do zero
POST   /api/my/templates/{id}/reset-to-default  # ← NOVO: sobrescreve jsonSchema com o do template de origem

# Templates padrão do sistema (leitura para CLIENT, leitura+edição para ADMIN)
GET    /api/system/templates                  # lista templates padrão (CLIENT vê, não edita)

# Assets (imagens usadas nos templates)
POST   /api/my/assets/image                   # upload de imagem (fundo, logo) → retorna URL
```

### 5.3 Apenas ADMIN

```
# Usuários e Revistas
GET    /api/admin/users
POST   /api/admin/users                # ao criar CLIENT, clona templates padrão automaticamente
PUT    /api/admin/users/{id}
DELETE /api/admin/users/{id}
POST   /api/admin/magazines/{id}/users/{userId}    # vincula usuário à revista
DELETE /api/admin/magazines/{id}/users/{userId}    # desvincula

GET    /api/admin/magazines
POST   /api/admin/magazines
PUT    /api/admin/magazines/{id}
DELETE /api/admin/magazines/{id}

# Templates do SISTEMA (Admin edita; CLIENT apenas lê via /api/system/templates)
GET    /api/admin/templates/system
PUT    /api/admin/templates/system/{id}  # Admin edita o padrão → afeta quem fizer reset no futuro

# Notícias
GET    /api/admin/news?page=&size=
POST   /api/admin/news
PUT    /api/admin/news/{id}
DELETE /api/admin/news/{id}
POST   /api/admin/news/{id}/publish
POST   /api/admin/news/{id}/unpublish
POST   /api/admin/news/upload-image    # multipart/form-data

# Eventos
GET    /api/admin/events?page=&size=
POST   /api/admin/events
PUT    /api/admin/events/{id}
DELETE /api/admin/events/{id}
POST   /api/admin/events/{id}/publish
POST   /api/admin/events/{id}/unpublish
```

---

## 6. Estrutura de Diretórios — API (Spring Boot)

```
src/main/java/com/example/cert/
│
├── CertApplication.java
│
├── shared/
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   ├── WebMvcConfig.java          # expõe /uploads como recurso estático
│   │   └── EmailConfig.java
│   ├── security/
│   │   ├── JwtTokenProvider.java      # inclui userId e role no token
│   │   ├── JwtAuthenticationFilter.java
│   │   └── CustomUserDetailsService.java
│   ├── upload/
│   │   └── FileStorageService.java    # salva imagens em /uploads
│   └── exception/
│       ├── GlobalExceptionHandler.java
│       └── BusinessException.java
│
├── auth/
│   ├── AuthController.java
│   ├── AuthService.java
│   └── dto/
│       ├── LoginRequest.java
│       └── LoginResponse.java         # { token, role, userId, magazineIds }
│
├── user/
│   ├── UserController.java            # /api/admin/users + /api/my/magazines
│   ├── UserService.java               # clona templates ao criar CLIENT
│   ├── Usuario.java
│   ├── UserRole.java
│   ├── UserRepository.java
│   └── dto/
│       ├── CreateUserRequest.java
│       └── UserResponse.java
│
├── magazine/
│   ├── MagazineController.java
│   ├── MagazineService.java
│   ├── Magazine.java
│   ├── MagazineRepository.java
│   └── dto/
│       ├── CreateMagazineRequest.java
│       └── MagazineResponse.java
│
├── certificate/
│   ├── CertificateController.java
│   ├── CertificateService.java
│   ├── CertificateEmailService.java   # envia PDF por e-mail após geração
│   ├── GeneratePdfService.java        # legado: Thymeleaf + OpenHTMLToPDF
│   ├── Certificate.java
│   ├── CertificateStatus.java
│   ├── Person.java
│   ├── CertificateRepository.java
│   ├── PersonRepository.java
│   └── dto/
│       ├── CertificateRequest.java    # { templateId, magazineId, volume, number, recipients }
│       ├── RecipientRequest.java      # { name, email, cpf }
│       └── CertificateResponse.java
│
├── template/
│   ├── TemplateController.java        # /api/my/templates
│   ├── TemplateAdminController.java   # /api/admin/templates/system
│   ├── TemplateService.java
│   ├── CertificateTemplate.java       # Entidade — owner = Usuario
│   ├── TemplateRepository.java
│   └── dto/
│       ├── TemplateResponse.java      # id, name, type, jsonSchema
│       └── SaveTemplateRequest.java   # { name, jsonSchema }
│
├── news/
│   ├── NewsPublicController.java      # GET /api/news (sem auth)
│   ├── NewsAdminController.java       # CRUD /api/admin/news (ADMIN only)
│   ├── NewsService.java
│   ├── NewsPost.java
│   ├── NewsRepository.java
│   └── dto/
│       ├── CreateNewsRequest.java
│       └── NewsResponse.java
│
└── event/
    ├── EventPublicController.java
    ├── EventAdminController.java
    ├── EventService.java
    ├── Event.java
    ├── EventRepository.java
    └── dto/
        ├── CreateEventRequest.java
        └── EventResponse.java
```

---

## 7. Estrutura de Diretórios — App (Angular)

```
src/app/
│
├── core/
│   ├── guards/
│   │   ├── auth.guard.ts
│   │   └── role.guard.ts
│   ├── interceptors/
│   │   └── auth.interceptor.ts
│   └── services/
│       ├── auth.service.ts
│       └── upload.service.ts
│
├── shared/
│   ├── components/
│   │   ├── navbar/
│   │   ├── footer/
│   │   ├── spinner/
│   │   ├── pagination/
│   │   └── image-upload-button/
│   └── models/
│       ├── certificate.model.ts
│       ├── template.model.ts          # interface PdfmeTemplate { id, name, type, jsonSchema }
│       ├── magazine.model.ts
│       ├── news.model.ts
│       └── event.model.ts
│
├── public/                            # Sem autenticação
│   ├── layout/
│   ├── home/
│   ├── news/
│   │   ├── news-list.component.ts
│   │   └── news-detail.component.ts
│   ├── events/
│   │   └── events-list.component.ts
│   └── certificates/
│       ├── validate/                  # (já existe — mover aqui)
│       └── search/                   # [NOVO] busca por nome/email
│
├── client/                            # CLIENT ou ADMIN
│   ├── layout/
│   │   └── client-layout.component.ts
│   ├── magazine-switcher/             # troca de revista ativa no painel
│   ├── certificates/
│   │   ├── issue/
│   │   └── history/
│   └── templates/                    # [NOVO — Sprint 3]
│       ├── template-list/
│       │   └── template-list.component.ts
│       └── template-editor/
│           ├── template-editor.component.ts   # PDFME Designer integrado
│           ├── template-editor.component.html
│           └── template-editor.component.css
│
├── admin/
│   ├── layout/
│   ├── dashboard/
│   ├── magazines/
│   ├── users/
│   │   ├── user-list.component.ts
│   │   └── user-form.component.ts
│   ├── news/
│   │   ├── news-list.component.ts
│   │   └── news-form.component.ts    # editor rico (ngx-quill)
│   └── events/
│       ├── event-list.component.ts
│       └── event-form.component.ts
│
└── auth/
    └── login/
```

### Roteamento raiz (app.routes.ts)

```typescript
export const routes: Routes = [
  {
    path: '',
    loadChildren: () => import('./public/public.routes').then(r => r.PUBLIC_ROUTES)
  },
  {
    path: 'auth',
    loadChildren: () => import('./auth/auth.routes').then(r => r.AUTH_ROUTES)
  },
  {
    path: 'client',
    canActivate: [authGuard],           // CLIENT ou ADMIN
    loadChildren: () => import('./client/client.routes').then(r => r.CLIENT_ROUTES)
  },
  {
    path: 'admin',
    canActivate: [authGuard, roleGuard('ADMIN')],
    loadChildren: () => import('./admin/admin.routes').then(r => r.ADMIN_ROUTES)
  },
  { path: '**', redirectTo: '' }
];
```

---

## 8. Integração do PDFME no Angular

### Instalação

```bash
npm install @pdfme/ui @pdfme/generator @pdfme/common @pdfme/schemas
```

### Componente do Editor

```typescript
// template-editor.component.ts
import { Component, ElementRef, ViewChild, AfterViewInit, Input, inject, signal } from '@angular/core';
import { Designer } from '@pdfme/ui';
import { TemplateService } from '../../services/template.service';

@Component({
  selector: 'app-template-editor',
  standalone: true,
  templateUrl: './template-editor.component.html'
})
export class TemplateEditorComponent implements AfterViewInit {
  @ViewChild('designerContainer') container!: ElementRef;
  @Input() templateId!: number;

  private designer!: Designer;
  private templateService = inject(TemplateService);
  saving = signal(false);

  ngAfterViewInit() {
    this.templateService.getById(this.templateId).subscribe(template => {
      this.designer = new Designer({
        domContainer: this.container.nativeElement,
        template: JSON.parse(template.jsonSchema),
        options: { lang: 'pt' }
      });
    });
  }

  save() {
    this.saving.set(true);
    const jsonSchema = JSON.stringify(this.designer.getTemplate());
    this.templateService.save(this.templateId, jsonSchema).subscribe(() => {
      this.saving.set(false);
    });
  }

  uploadBackground(file: File) {
    const reader = new FileReader();
    reader.onload = () => {
      const tpl = this.designer.getTemplate();
      tpl.basePdf = reader.result as string;
      this.designer.updateTemplate(tpl);
    };
    reader.readAsDataURL(file);
  }
}
```

### Geração do PDF (client-side)

```typescript
// issue-cert.component.ts
import { generate } from '@pdfme/generator';

async onSubmit() {
  // 1. Chama API para registrar no banco e obter os UUIDs
  const saved = await this.certService.register(this.formData).toPromise();

  // 2. Monta inputs do PDFME com os UUIDs retornados
  const template = JSON.parse(this.selectedTemplate.jsonSchema);
  const inputs = saved.map(cert => ({
    name: cert.person.name,
    magazine: cert.magazine.name,
    issn: cert.magazine.issn,
    date: new Date(cert.createdAt).toLocaleDateString('pt-BR'),
    uuid: cert.validationCode,
  }));

  // 3. Gera o PDF no browser
  const pdfBytes = await generate({ template, inputs });

  // 4. Envia o PDF para a API disparar os e-mails
  await this.certService.sendEmails(saved.map(c => c.id), pdfBytes).toPromise();

  // 5. Download local opcional
  const blob = new Blob([pdfBytes], { type: 'application/pdf' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a'); a.href = url; a.download = 'certificados.pdf'; a.click();
}
```

---

## 9. E-mail com Gmail SMTP

### `.env` da API

```env
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=seuemail@gmail.com
MAIL_PASSWORD=xxxx_xxxx_xxxx_xxxx   # App Password — NÃO é a senha da conta Google
```

### `application.yaml`

```yaml
spring:
  mail:
    host: ${MAIL_HOST:smtp.gmail.com}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true
```

> **Como gerar a App Password do Gmail:**
> myaccount.google.com → Segurança → Verificação em duas etapas → Senhas de app

---

## 10. Prioridade de Implementação — Sprints

### Sprint 1 — Fundação do Domínio
- [ ] Adicionar `role` e `@ManyToMany magazines` em `Usuario`
- [ ] Criar tabela de junção `user_magazine`
- [ ] Criar entidade `CertificateTemplate` com `owner = Usuario`
- [ ] Criar enums `UserRole` e `CertificateStatus`
- [ ] Adicionar `status`, `recipientEmail`, `template` em `Certificate`
- [ ] Completar `Person` com Lombok
- [ ] Reorganizar pacotes por módulo (mover arquivos existentes)
- [ ] Adicionar `@PreAuthorize` nos controllers existentes
- [ ] Criar `RoleGuard` no Angular

### Sprint 2 — Serviço de E-mail e Assets
- [ ] `spring-boot-starter-mail` no `pom.xml`
- [ ] Configurar Gmail SMTP no `.env`
- [ ] `CertificateEmailService` — envia PDF em anexo
- [ ] `FileStorageService` — salva imagens em `/uploads`
- [ ] `WebMvcConfig` — expõe `/uploads` como recurso estático

### Sprint 3 — Editor de Templates (PDFME)
- [ ] Criar templates padrão do sistema em JSON (converter HTMLs atuais)
- [ ] `TemplateService` com lógica de clonagem no onboarding
- [ ] Endpoints `/api/my/templates` (CRUD completo)
- [ ] `npm install @pdfme/ui @pdfme/generator @pdfme/common @pdfme/schemas`
- [ ] Componente `template-editor` com PDFME Designer
- [ ] Tela de listagem de templates com preview
- [ ] Geração do PDF client-side na tela de emissão

### Sprint 4 — Busca Pública de Certificados
- [ ] Query no repository por `person.name ILIKE` ou `person.email`
- [ ] Endpoint `GET /api/certificates/search`
- [ ] Componente `search-cert` com paginação no Angular

### Sprint 5 — Site Institucional (Notícias e Eventos)
- [ ] Entidades `NewsPost` e `Event` + scripts de migration SQL
- [ ] Controllers públicos (apenas `published = true`)
- [ ] Controllers admin (CRUD + publish/unpublish)
- [ ] Upload de imagem de capa para notícias
- [ ] Editor rico no admin (`npm install ngx-quill`)
- [ ] Páginas públicas no Angular: home, notícias, eventos

### Sprint 6 — Polimento do Painel
- [ ] `magazine-switcher` — dropdown para trocar a revista ativa no painel
- [ ] Dashboard admin com estatísticas (certificados emitidos, enviados, com falha)
- [ ] CRUD completo de revistas e usuários no admin
- [ ] Tela de vinculação `admin/magazines/{id}/users`

---

## 11. Princípios SOLID Aplicados

| Princípio | Aplicação no projeto |
|---|---|
| **S** — Single Responsibility | `CertificateService` registra; `CertificateEmailService` envia e-mail; `GeneratePdfService` gera o PDF |
| **O** — Open/Closed | Novo tipo de certificado = novo template no banco, sem alterar código |
| **L** — Liskov | `FileStorageService` pode ter impl `LocalFileStorage` ou `S3FileStorage` com a mesma interface |
| **I** — Interface Segregation | `NewsPublicController` (só publicados) separado de `NewsAdminController` (tudo) |
| **D** — Dependency Inversion | `EmailService` como interface; `GmailEmailService` como implementação injetada via Spring |
