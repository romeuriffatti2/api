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
                .validationCode(certificate.getValidationCode())
                .magazineResponse(MagazineMapper.toResponse(certificate.getMagazine()))
                .volume(certificate.getVolume())
                .number(certificate.getNumber())
                .type(certificate.getType())
                .metadata(certificate.getMetadata())
                .createdAt(certificate.getCreatedAt() != null ? certificate.getCreatedAt().toString() : null)
                .build();
    }

    public static Certificate toEntity(CertificateItemRequest itemRequest, Magazine magazine, String volume, String number, String type) {

        return Certificate.builder()
                .name(itemRequest.getName())
                .validationCode(itemRequest.getValidationCode())
                .magazine(magazine)
                .volume(volume)
                .number(number)
                .type(type)
                .metadata(itemRequest.getMetadata())
                .build();
    }

}
