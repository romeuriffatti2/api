---
description: How to run the Spring Boot backend
---

This workflow allows you to quickly start the backend. 

> [!IMPORTANT]
> This project requires **Java 17** or higher. If you see errors in the terminal, it's because your default Java is an older version. I've created a script to fix this for you.

// turbo
1. Run the Spring Boot application using the helper script:
```powershell
.\run.ps1
```

2. Alternatively, you can use the manual command (if you have Java 17 in your PATH):
```powershell
.\mvnw.cmd spring-boot:run
```

2. Alternatively, you can use the "Run" button in the "Run and Debug" sidebar (Ctrl+Shift+D) and select "CertApplication".

3. To check if the application is healthy:
```powershell
curl http://localhost:8080/actuator/health
```
