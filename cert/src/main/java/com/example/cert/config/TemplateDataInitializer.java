package com.example.cert.config; // ajuste o pacote se necessário

import com.example.cert.service.templates.InitializeTemplatesService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class TemplateDataInitializer implements CommandLineRunner {

    private final InitializeTemplatesService initializeTemplatesService;

    @Override
    public void run(String... args) {
        log.info("Inicializando/Atualizando templates padrão do sistema...");
        initializeTemplatesService.initializeSystemTemplates();
        log.info("Templates padrão inicializados com sucesso.");
    }
}
