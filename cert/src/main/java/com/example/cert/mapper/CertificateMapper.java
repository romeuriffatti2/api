package com.example.cert.mapper;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import com.example.cert.Response.CertificateResponse;
import com.example.cert.domain.Certificate;
import com.example.cert.domain.Magazine;
import com.example.cert.request.CertificateItemRequest;

@Component
@AllArgsConstructor
public class CertificateMapper {

    public CertificateResponse toResponse(Certificate certificate) {
        return CertificateResponse.builder()
                .id(certificate.getId())
                .name(certificate.getName())
                .validationCode(certificate.getValidation_code())
                .magazineResponse(MagazineMapper.toResponse(certificate.getMagazine()))
                .build();
    }

    public static Certificate toEntity(CertificateItemRequest itemRequest, Magazine magazine) {

        return Certificate.builder()
                .name(itemRequest.getName())
                .validation_code(itemRequest.getValidationCode())
                .magazine(magazine)
                .build();
    }

}
