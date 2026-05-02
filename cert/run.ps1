# Help script to run the backend with the correct JDK (Java 17)

# Se a variável JAVA_HOME não estiver definida, o mvnw tentará usar o java no PATH.
if (-not $env:JAVA_HOME) {
    Write-Host "Aviso: JAVA_HOME não está definido. Certifique-se de que o Java 17 está instalado e no PATH." -ForegroundColor Yellow
} else {
    Write-Host "Usando JAVA_HOME em: $env:JAVA_HOME" -ForegroundColor Green
}

.\mvnw.cmd spring-boot:run
