Corrigir DataInitializer.java para PDFME v6
Diagnóstico
Dois bugs independentes foram identificados no DataInitializer.java, cada um causando um erro diferente.

Bug 1 — JSON com Sintaxe Inválida (causa do SyntaxError: Expected ',' or '}')
Causa Raiz
O DataInitializer.java usa String.format() com text blocks Java para construir os JSONs. Nos métodos buildPublicacaoSchema(), buildDossieSchema() e buildAceiteSchema(), o campo content do body inclui aspas duplas em volta de variáveis de template, escritas em Java como \\\"{{articleTitle}}\\\".

No Java, \\\" dentro de uma string literal produz os dois caracteres \" (barra invertida + aspas duplas). Isso gera no banco um JSON assim:

json
"content": "... intitulado \"{{articleTitle}}\" ..."
Isso seria JSON válido se o frontend lesse a string diretamente. O problema está em template-editor.component.ts, linha 56:

typescript
const cleanJson = template.jsonSchema ? template.jsonSchema.replace(/\\\"/g, '"') : '{}';
Esta regex substitui \" por " em toda a string JSON. Ao fazer isso, a aspa que era válida como escape dentro da string JSON torna-se uma aspa literal, quebrando a estrutura do JSON. O resultado é:

"content": "... intitulado "{{articleTitle}}" ..."
//                          ^ termina a string aqui ← JSON inválido!
Onde ocorre no DataInitializer
Método	Conteúdo problemático
buildPublicacaoSchema()	intitulado \\\"{{articleTitle}}\\\" e {{accessLink}} sem aspas mas com dois \\n\\n
buildDossieSchema()	Dossiê Temático intitulado \\\"{{dossieTitle}}\\\"
buildAceiteSchema()	artigo intitulado \\\"{{articleTitle}}\\\"
Solução para Bug 1
Remover as aspas duplas dos conteúdos de template nas strings de body. Substituir \\\"{{variavel}}\\\" por apenas {{variavel}} (ou usar aspas curvas tipográficas "{{variavel}}" como alternativa estética).

IMPORTANT

A regex replace(/\\\"/g, '"') no template-editor.component.ts é um anti-pattern que deve ser removida em paralelo (ou antes) dessa correção. Se o DataInitializer parar de gerar \" nos conteúdos, a regex não causará dano imediato, mas continuará sendo um código incorreto que quebrará qualquer template salvo pelo editor que contenha aspas.

Bug 2 — Propriedades Incompatíveis com PDFME v6 (causaria erros de renderização/validação)
Causa Raiz
O PDFME v6.0.6 (versão instalada, confirmada no package-lock.json) removeu e renomeou propriedades do schema de texto em relação a versões anteriores (v3/v4). O DataInitializer gera JSONs com propriedades que o PDFME v6 não reconhece, confirmado inspecionando @pdfme/schemas/dist/text/types.d.ts.

Comparação: propriedades geradas vs. PDFME v6
Propriedade no DataInitializer	Status no PDFME v6	Correção
"fontStyle": "bold"	❌ Removida — não existe no TextSchema	Remover. Negrito é controlado pelo fontName ou não suportado diretamente no schema
"textTransform": "uppercase"	❌ Removida — não existe no TextSchema	Remover totalmente
"alignment": "justified"	❌ Inválido — o tipo aceito é 'left' | 'center' | 'right' | 'justify'	Alterar para "justify"
O TextSchema do PDFME v6 aceita: fontName, alignment, verticalAlignment, fontSize, lineHeight, strikethrough, underline, characterSpacing, dynamicFontSize, fontColor, backgroundColor.

NOTE

O PDFME v6 usa milímetros (mm) como unidade para position.x, position.y, width e height. Os valores atuais (ex: x:20, y:60, width:257 para A4 landscape de 297mm) estão corretos — não é necessário converter unidades. A suspeita inicial sobre unidades não procede para essa versão.

Alterações propostas no DataInitializer.java
1. BLANK_PDF (já corrigida na sessão anterior)
O base64 corrompido já foi corrigido para um PDF válido gerado pelo pdf-lib.

2. Método buildParticipacaoSchema() e todos os outros — remover fontStyle e textTransform
Em todos os campos title que têm:

java
"fontStyle": "bold",
"alignment": "center", "textTransform": "uppercase", "characterSpacing": 2
Remover "fontStyle": "bold" e "textTransform": "uppercase":

java
"alignment": "center", "characterSpacing": 2
3. Métodos buildPublicacaoSchema(), buildDossieSchema(), buildAceiteSchema() — corrigir "justified" → "justify"
java
// Antes
"alignment": "justified"
// Depois
"alignment": "justify"
4. Métodos buildPublicacaoSchema(), buildDossieSchema(), buildAceiteSchema() — remover aspas duplas escapadas dos conteúdos
buildPublicacaoSchema() — campo body:

java
// Antes
"content": "...intitulado \\\"{{articleTitle}}\\\", de autoria de..."
// Depois
"content": "...intitulado {{articleTitle}}, de autoria de..."
buildDossieSchema() — campo body:

java
// Antes
"content": "...Dossiê Temático intitulado \\\"{{dossieTitle}}\\\"..."
// Depois
"content": "...Dossiê Temático {{dossieTitle}}..."
buildAceiteSchema() — campo body:

java
// Antes
"content": "...artigo intitulado \\\"{{articleTitle}}\\\"..."
// Depois
"content": "...artigo intitulado {{articleTitle}}..."
Alteração necessária no código Angular (fora do DataInitializer)
WARNING

Esta correção precisa ser feita junto com as alterações do DataInitializer para que o sistema funcione consistentemente.

[MODIFY] template-editor.component.ts (linha 56)
diff
- const cleanJson = template.jsonSchema ? template.jsonSchema.replace(/\\\"/g, '"') : '{}';
+ const cleanJson = template.jsonSchema ?? '{}';
Remover o .replace() que destrói o JSON ao des-escapar as aspas.

Plano de execução
Apagar os registros do banco (system + user templates)
Aplicar as 4 correções acima no DataInitializer.java
Remover o .replace() no template-editor.component.ts
Reiniciar o backend → DataInitializer recria os templates corretamente
O Angular recarrega automaticamente (já está com ng serve)
Testar geração de certificado do tipo parecerista
Verificação
Abrir o editor de template → não deve aparecer erro de JSON parse
Clicar em "Gerar Certificados" → não deve aparecer PDFInvalidObjectParsingError
O PDF gerado deve ter os campos {{articleTitle}}, {{dossieTitle}} visíveis no conteúdo (sem aspas, mas sem erros)
