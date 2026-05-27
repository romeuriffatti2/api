# Guia de Deploy na VPS

Este guia documenta o passo a passo para enviar as alterações feitas localmente (Frontend e Backend) para a sua VPS, baseando-se na estrutura de arquivos e no fluxo que você já utilizou.

---

## 1. Preparação Local (Build)

Antes de enviar os arquivos, você precisa gerar as versões de produção do código na sua máquina.

### Backend (Spring Boot)
1. Abra o terminal na pasta do backend (`api/api/cert`).
2. Execute o comando para limpar e gerar o novo arquivo `.jar`:
   ```powershell
   .\mvnw clean package
   ```
3. O arquivo gerado estará em `target/cert-0.0.1-SNAPSHOT.jar`.

### Frontend (Angular)
1. Abra o terminal na pasta do frontend (`app/app`).
2. Execute o comando de build de produção:
   ```powershell
   ng build
   ```
3. Os arquivos compilados estarão na pasta `dist/app/browser`.
4. Compacte todo o conteúdo de dentro da pasta `dist/app/browser` para o arquivo `frontend.zip` na raiz do frontend usando o PowerShell:
   ```powershell
   Compress-Archive -Path dist/app/browser/* -DestinationPath frontend.zip -Force
   ```

---

## 2. Envio dos Arquivos para a VPS

Você pode enviar os arquivos diretamente via terminal usando o comando **SCP** (Secure Copy) ou utilizar um cliente gráfico (como FileZilla ou WinSCP).

### Opção A: Via Terminal (Recomendado)

Abra o terminal na sua máquina local e execute os comandos abaixo. 

1. **Enviar o Backend (JAR)**:
   Abra o terminal na pasta `api/api/cert` e execute (será solicitada a senha da VPS):
   ```powershell
   scp -P 22022 "target/cert-0.0.1-SNAPSHOT.jar" root@108.174.146.114:/opt/certificados/backend/cert-0.0.1-SNAPSHOT.jar
   ```

2. **Enviar o Frontend (ZIP)**:
   Abra o terminal na pasta `app/app` (onde você gerou o `frontend.zip`) e execute (será solicitada a senha da VPS):
   ```powershell
   scp -P 22022 "frontend.zip" root@108.174.146.114:/opt/certificados/frontend.zip
   ```

---

### Opção B: Via Cliente Gráfico (SFTP/SCP)

Utilize o seu cliente SFTP/SCP preferido (FileZilla, WinSCP) conectando na porta **22022**:

1. **Backend**: Faça o upload do arquivo local `target/cert-0.0.1-SNAPSHOT.jar` substituindo o arquivo existente no caminho `/opt/certificados/backend/cert-0.0.1-SNAPSHOT.jar` da VPS.
2. **Frontend**: Faça o upload do arquivo `frontend.zip` para a pasta raiz do projeto na VPS (`/opt/certificados/frontend.zip`).

---

## 3. Atualização na VPS

Acesse a sua VPS via SSH conectando na porta **22022**:
```bash
ssh -p 22022 root@108.174.146.114
```

Execute os passos abaixo após conectar:

### Atualizando o Frontend
1. Navegue até o diretório do projeto:
   ```bash
   cd /opt/certificados
   ```
2. Remova os arquivos antigos da pasta `frontend`:
   ```bash
   rm -rf frontend/*
   ```
3. Descompacte o novo `frontend.zip` dentro da pasta `frontend`:
   ```bash
   unzip frontend.zip -d frontend/
   ```
4. (Opcional) Remova o arquivo zip para limpar espaço:
   ```bash
   rm frontend.zip
   ```

### Atualizando o Backend
Como você já substituiu o arquivo `.jar` via SCP, agora só precisa reiniciar o container Docker do backend.

1. Navegue até o diretório do projeto (se já não estiver):
   ```bash
   cd /opt/certificados
   ```
2. Pare e remova o container atual do backend (caso esteja usando docker-compose):
   ```bash
   docker stop certificados-backend
   docker rm certificados-backend
   ```
3. Suba o container novamente recriando-o com o novo `.jar` (o Docker vai usar o novo jar na inicialização):
   ```bash
   docker run -d \
    --name certificados-backend \
    --network certificados-network \
    -p 8080:8080 \
    -v /opt/certificados/backend/cert-0.0.1-SNAPSHOT.jar:/app/app.jar \
    -v /opt/certificados/backend/src/main/resources/application-prod.yaml:/app/config/application-prod.yaml \
    -v /opt/certificados/uploads:/app/uploads \
    certificados-backend
   ```
   *(Nota: Se o seu serviço no docker-compose tiver outro nome, substitua `backend` pelo nome correto).*
4. Verificar logs do build do backend:

   ```bash
      docker logs -f certificados-backend

   ```

---

## 4. Verificação

- Acesse a aplicação pelo navegador e pressione `Ctrl + F5` para limpar o cache e garantir que o novo frontend carregou.
- Verifique os logs do backend para garantir que iniciou corretamente sem erros de banco de dados ou ambiente:
  ```bash
  docker-compose logs -f backend
  ```
