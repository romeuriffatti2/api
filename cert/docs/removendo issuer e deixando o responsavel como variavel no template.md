# Remoção da Entidade Issuer e Refatoração

Seguindo as suas diretrizes, realizei as alterações sem utilizar os comandos de reconstrução do Spring (`ddl-auto` continua como `validate`), permitindo que você recrie o banco manualmente a partir do `db_schema.sql` atualizado. Todos os passos previstos no plano de implementação foram concluídos.

## O que foi alterado:

### Banco de Dados
- **[db_schema.sql](file:///c:/Users/romeu/Desktop/cert/api/api/cert/docs/db_schema.sql):**
  - Removido o bloco `CREATE TABLE issuer`.
  - Removida a coluna `issuer_id` e a foreign key associada na tabela `certificate`.
  - Adicionada a coluna `issuer_name VARCHAR(255)` na tabela `certificate_template`. 

> [!NOTE]
> Você pode usar o `db_schema.sql` atualizado para dropar o banco antigo e recriar as tabelas. Lembre-se de deletar e criar novamente usando este script atualizado.

### Backend (Spring Boot)
- **Deleção de Arquivos:** As classes `Issuer.java`, `IssuerRepository.java`, `IssuerService.java`, `IssuerController.java` e `IssuerResponse.java` foram completamente deletadas do projeto.
- **Entidades:** O relacionamento com `Issuer` em `Certificate.java` foi removido. A propriedade `issuerName` (String) foi adicionada na entidade `CertificateTemplate.java`.
- **DTOs e Mappers:** 
  - `SaveTemplateRequest` e `TemplateResponse` agora recebem e devolvem a propriedade `issuerName`.
  - `TemplateService` foi atualizado para persistir esse campo em todos os métodos relevantes (criação, clonagem, atualização).
  - O mapper e o DTO de `Certificate` tiveram todas as dependências de `Issuer` extirpadas.

> [!IMPORTANT]
> A responsabilidade sobre o emissor passou para a edição dos templates. Quando os templates forem salvos pela API, basta enviar `"issuerName": "Nome do Emissor"` junto com o JSON Schema no corpo da requisição de `SaveTemplateRequest`.

Com isso, o back-end está preparado para esta nova estrutura sem o modelo relacional rígido de emissores. Aguardo as próximas informações para continuar a reestruturação!
