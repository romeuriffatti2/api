package com.example.cert.service;

import com.example.cert.Response.CertificateResponse;
import com.example.cert.domain.Certificate;
import com.example.cert.domain.Magazine;
import com.example.cert.mapper.CertificateMapper;
import com.example.cert.repository.CertificateRepository;
import com.example.cert.repository.MagazineRepository;
import com.example.cert.request.CertificateRequest;
import com.example.cert.utils.GeneratePdfService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class CertificateService {

    private final CertificateRepository certificateRepository;
    private final CertificateMapper certificateMapper;
    private final MagazineRepository magazineRepository;

    private final GeneratePdfService generatePdfService;

    public Page<CertificateResponse> getAllCertificates(Pageable pageable) {
        return certificateRepository
                .findAll(pageable)
                .map(certificateMapper::toResponse);
    }

    public byte[] create(CertificateRequest certificateRequest) {

        Magazine magazine = magazineRepository.findById(certificateRequest.getMagazineId()).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Revista não encontrada"));

        List<Certificate> savedCertificates = new ArrayList<>();
        certificateRequest.getCertificates().forEach(item -> {
            Certificate certificate = CertificateMapper.toEntity(
                item, magazine, certificateRequest.getVolume(), certificateRequest.getNumber(), null, certificateRequest.getType()
            );
            savedCertificates.add(certificateRepository.save(certificate));
        });

        return generatePdfService.generatePdf(savedCertificates);
    }



    @Transactional(readOnly = true)
    public CertificateResponse validateCertificate(String code) {
        return certificateRepository.findByValidationCode(UUID.fromString(code))
                .map(certificateMapper::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Certificado não encontrado"));
    }

    @Transactional(readOnly = true)
    public byte[] downloadCertificate(String code) {
        Certificate certificate = certificateRepository.findByValidationCode(UUID.fromString(code))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Certificado não encontrado"));
        return generatePdfService.generatePdf(List.of(certificate));
    }
}
