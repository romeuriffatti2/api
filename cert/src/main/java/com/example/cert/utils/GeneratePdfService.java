package com.example.cert.utils;

import com.example.cert.domain.Certificate;
import com.example.cert.domain.Magazine;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.AllArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.List;

@Service
@AllArgsConstructor
public class GeneratePdfService {

    private final TemplateEngine templateEngine;
    private final ResourceLoader resourceLoader;

    private String getTemplateName(String type) {
        if (type == null) return "certificate";
        switch(type) {
            case "parecerista": return "parecerist_certificate";
            case "corpo-editorial":
            case "comite-tematico": return "editorial_certificate";
            case "dossie": return "dossie_certificate";
            case "aceite": return "aceite_certificate";
            case "publicacao": return "publication_certificate";
            default: return "certificate";
        }
    }

    public byte[] generatePdf(List<Certificate> certificates) {
        Context context = new Context();
        context.setVariable("certificates", certificates);

        if (!certificates.isEmpty()) {
            Magazine magazine = certificates.get(0).getMagazine();
            if (magazine != null) {
                try {
                    Resource resource = resourceLoader.getResource("classpath:static/logos/" + magazine.getId() + ".png");
                    if (resource.exists()) {
                        try (InputStream is = resource.getInputStream()) {
                            byte[] bytes = is.readAllBytes();
                            context.setVariable("logoBase64", Base64.getEncoder().encodeToString(bytes));
                        }
                    }
                } catch (Exception e) {
                    // Ignora erro se o logo não existir
                }
            }
        }
        String templateName = certificates.isEmpty() ? "certificate" : getTemplateName(certificates.get(0).getType());
        String htmlContent = templateEngine.process(templateName, context);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(htmlContent, null);
            builder.toStream(outputStream);
            builder.run();
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erro ao gerar PDF", e);
        }
    }
}
