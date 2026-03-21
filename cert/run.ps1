# Help script to run the backend with the correct JDK (Java 17)

$JdkPath = "C:\Users\DaniMateus\.jdks\ms-17.0.16"

if (Test-Path $JdkPath) {
    Write-Host "Using JDK 17 from: $JdkPath" -ForegroundColor Green
    $env:JAVA_HOME = $JdkPath
} else {
    Write-Host "Warning: JDK 17 not found at $JdkPath. Please ensure Java 17 is installed." -ForegroundColor Yellow
}

.\mvnw.cmd spring-boot:run
