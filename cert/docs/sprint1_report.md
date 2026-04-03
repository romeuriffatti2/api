# Sprint 1 — Relatório de Execução: Fundação do Domínio

**Data:** 01/04/2026  
**Status:** ✅ Concluído

---

## Resumo

Sprint 1 executado com sucesso. Todos os novos tipos, entidades e configurações de autorização foram adicionados ao projeto sem quebrar o código existente. A estratégia adotada foi **aditiva** — novos arquivos criados e campos adicionados às entidades existentes, mantendo total compatibilidade com o código legado.

---

## Alterações Realizadas

### API — Spring Boot

#### ✅ Novos Enums

| Arquivo | Localização | Descrição |
|---|---|---|
| `UserRole.java` | `domain/` | `ADMIN` e `CLIENT` — define o papel do usuário na plataforma |
| `CertificateStatus.java` | `domain/` | `GENERATED`, `EMAIL_SENT`, `EMAIL_FAILED` — ciclo de vida do envio por e-mail |

#### ✅ Nova Entidade

**`CertificateTemplate.java`** (`domain/`)
- Campos: `id`, `name`, `type`, `systemDefault`, `active`, `jsonSchema`, `owner`, `sourceTemplateId`, `createdAt`, `updatedAt`
- `owner` → `@ManyToOne` com `Usuario` (null = template padrão do sistema)
- `sourceTemplateId` → ID do template padrão de origem, habilita o "reset para o padrão"
- `jsonSchema` → campo `TEXT` que armazena o JSON do PDFME Designer

#### ✅ `Person.java` recriado (`domain/`)
- Lombok completo: `@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor`
- Campos: `id`, `name`, `email` (unique), `cpf` (unique, validado com `@CPF`)

#### ✅ `Usuario.java` atualizado (`domain/`)
Campos adicionados:
- `role: UserRole` — `@Enumerated(STRING)`, default `CLIENT`, `@Builder.Default`
- `magazines: Set<Magazine>` — `@ManyToMany` via tabela `user_magazine`
- `templates: List<CertificateTemplate>` — `@OneToMany` mapeado por `owner`

#### ✅ `Certificate.java` atualizado (`domain/`)
Campos adicionados:
- `person: Person` — `@ManyToOne`, destinatário estruturado
- `recipientEmail: String` — cópia do e-mail para envio sem JOIN
- `status: CertificateStatus` — `@Enumerated(STRING)`, default `GENERATED`
- `template: CertificateTemplate` — `@ManyToOne`, rastreabilidade do template usado

#### ✅ Novos Repositórios

| Arquivo | Localização | Métodos chave |
|---|---|---|
| `CertificateTemplateRepository.java` | `repository/` | `findBySystemDefaultTrue()`, `findByOwner()`, `findByIdAndOwner()` |
| `PersonRepository.java` | `repository/` | `findByEmail()`, `findByCpf()` |

#### ✅ `CustomUserDetailsService.java` atualizado (`security/`)
- Agora inclui a `UserRole` como `GrantedAuthority` (`ROLE_ADMIN`, `ROLE_CLIENT`)
- Isso habilita o uso de `@PreAuthorize("hasRole('ADMIN')")` nos controllers

#### ✅ `SecurityConfig.java` atualizado (`config/`)
- Adicionada a anotação `@EnableMethodSecurity` — habilita `@PreAuthorize` globalmente
- Novas rotas públicas (sem autenticação):
  - `/api/certificates/search`
  - `/api/news/**`
  - `/api/events/**`
  - `/api/system/templates`
  - `/uploads/**`

#### ✅ `CertificateController.java` atualizado (`controller/`)
- `GET /list` → `@PreAuthorize("hasAnyRole('ADMIN', 'CLIENT')")`
- `POST /generate` → `@PreAuthorize("hasAnyRole('ADMIN', 'CLIENT')")`
- `GET /validate/**` e `GET /download/**` → permanecem públicos (sem `@PreAuthorize`)

---

### Angular — Frontend

#### ✅ `role.guard.ts` criado (`guards/`)
- Função `roleGuard(...allowedRoles: string[]): CanActivateFn`
- Uso: `canActivate: [authGuard, roleGuard('ADMIN')]`
- Lê a role via `AuthService.getRole()`
- Redireciona para `/` se o usuário não tem a role necessária

#### ✅ `auth.service.ts` atualizado (`services/`)
Novos recursos:
- `getRole(): string | null` — lê do `sessionStorage` ou decodifica o JWT
- `isAdmin(): boolean` — atalho para verificação de role
- `isClient(): boolean` — atalho para verificação de role
- `logout()` agora remove também o item `role` do `sessionStorage`
- `login()` agora salva `response.role` no `sessionStorage` quando retornado pela API

---

## Tabelas Criadas no Banco (DDL automático via Hibernate)

> O Hibernate criará/atualizará as tabelas automaticamente com base nas entidades.
> Verifique o `ddl-auto` no `.env` (`validate` em produção, `update` em dev).

| Tabela | Alteração |
|---|---|
| `usuario` | Novas colunas: `role`, tabela de junção `user_magazine` |
| `certificate` | Novas colunas: `person_id`, `recipient_email`, `status`, `template_id` |
| `certificate_template` | **Nova tabela** |
| `person` | **Nova tabela** |
| `user_magazine` | **Nova tabela de junção** (Many-to-Many usuário ↔ revista) |

> [!WARNING]
> Se o banco já possui dados e `ddl-auto=validate`, você precisará executar migrations SQL manualmente para adicionar as novas colunas antes de reiniciar a API.

---

## Pendências para Próximos Sprints

| Item | Sprint |
|---|---|
| Incluir `role` no JWT gerado pelo `JwtTokenProvider` | Sprint 1.5 (crítico antes do deploy) |
| Reorganização de pacotes por módulo (authoria, certificate, magazine, etc.) | Sprint futuro (não prioritário, não quebra nada) |
| `UserService.createClient()` — lógica de clonagem de templates no onboarding | Sprint 3 |
| Endpoints `/api/my/templates` e `/api/admin/templates/system` | Sprint 3 |

---

## Observação — Role no JWT (✅ Resolvido na sequência do Sprint 1)

Os três arquivos foram atualizados para fechar o ciclo completo:

| Arquivo | O que mudou |
|---|---|
| `JwtTokenProvider.java` | `.claim("roles", roles)` — inclui as roles no payload do JWT |
| `JwtResponse.java` | Adicionado campo `role` + construtor `(token, email, role)` |
| `AuthController.java` | Busca a role do `UserRepository` após autenticação e inclui na resposta |

**Fluxo resultante:**
```
POST /api/auth/login
  → response: { token: "eyJ...", email: "...", role: "ADMIN" }
     Angular: sessionStorage.setItem('role', response.role)
     AuthService.getRole() → "ADMIN"
     roleGuard('ADMIN') → ✅ acesso liberado
```
