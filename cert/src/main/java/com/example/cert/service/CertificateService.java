package com.example.cert.service;

import com.example.cert.Response.CertificateResponse;
import com.example.cert.domain.Certificate;
import com.example.cert.domain.Magazine;
import com.example.cert.mapper.CertificateMapper;
import com.example.cert.repository.CertificateRepository;
import com.example.cert.repository.MagazineRepository;
import com.example.cert.request.CertificateRequest;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class CertificateService {

    private final CertificateRepository certificateRepository;
    private final CertificateMapper certificateMapper;
    private final MagazineRepository magazineRepository;
    private final TemplateEngine templateEngine;

    public Page<CertificateResponse> getAllCertificates(Pageable pageable) {
        return certificateRepository
                .findAll(pageable)
                .map(certificateMapper::toResponse);
    }

    public byte[] create(CertificateRequest certificateRequest) {

        Magazine magazine = magazineRepository.findById(certificateRequest.getMagazineId()).orElseThrow(
                () ->new ResponseStatusException(HttpStatus.NOT_FOUND, "Revista não encontrada"));

        List<Certificate> savedCertificates = new ArrayList<>();

        certificateRequest.getCertificates().forEach(item -> {
            Certificate certificate = CertificateMapper.toEntity(item, magazine);
            savedCertificates.add(certificateRepository.save(certificate));
        });

        return generatePdf(savedCertificates);
    }

    private byte[] generatePdf(List<Certificate> certificates) {
        Context context = new Context();
        context.setVariable("certificates", certificates);
        String htmlContent = templateEngine.process("certificate", context);

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
    public CertificateResponse validateCertificate(String code) {
        return certificateRepository.findByValidation_code(UUID.fromString(code))
                .map(certificateMapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Certificado não encontrado"));
    }
}
