package com.example.cert.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Sprint 2 — Expõe o diretório de uploads como recurso estático HTTP.
 * <p>
 * Com esta configuração, qualquer arquivo salvo via FileStorageService em
 * ${app.upload.dir} fica acessível publicamente via GET /uploads/{filename}.
 * <p>
 * O SecurityConfig já tem .requestMatchers("/uploads/**").permitAll() para
 * garantir acesso sem JWT a estas URLs.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absolutePath = Paths.get(uploadDir).toAbsolutePath().normalize().toString();
        // Garante que o path termina com separador do SO
        String resourceLocation = "file:" + absolutePath + "/";

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(resourceLocation);
    }

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        // Aumenta o timeout para requisições async (envio de e-mail em background)
        configurer.setDefaultTimeout(60_000);
    }
}
