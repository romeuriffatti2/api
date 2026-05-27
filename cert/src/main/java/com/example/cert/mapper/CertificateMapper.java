package com.example.cert.mapper;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import com.example.cert.Response.CertificateResponse;
import com.example.cert.domain.Certificate;
import com.example.cert.domain.Magazine;
import com.example.cert.domain.Person;
import com.example.cert.request.CertificateItemRequest;

@Component
@AllArgsConstructor
public class CertificateMapper {

    /**
     * Converte Certificate para DTO de resposta.
     * Nome e e-mail são sempre lidos via person (fonte única de verdade).
     */
    public CertificateResponse toResponse(Certificate certificate) {
        String name = certificate.getPerson() != null
                ? certificate.getPerson().getName()
                : "—";

        return CertificateResponse.builder()
                .id(certificate.getId())
                .name(name)
                .validationCode(certificate.getValidationCode())
                .magazineResponse(MagazineMapper.toResponse(certificate.getMagazine()))
                .volume(certificate.getVolume())
                .number(certificate.getNumber())
                .type(certificate.getType())
                .metadata(certificate.getMetadata())
                .createdAt(certificate.getCreatedAt() != null ? certificate.getCreatedAt().toString() : null)
                .build();
    }

    /**
     * Converte request + contexto para entidade Certificate.
     * Recebe a Person já resolvida/persistida pela Service — o mapper não persiste nada.
     */
    public static Certificate toEntity(
            CertificateItemRequest itemRequest,
            Person person,
            Magazine magazine,
            String volume,
            String number,
            String type,
            com.example.cert.domain.CertificateTemplate template) {

        return Certificate.builder()
                .person(person)
                .validationCode(itemRequest.getValidationCode())
                .magazine(magazine)
                .volume(volume)
                .number(number)
                .type(type)
                .metadata(itemRequest.getMetadata())
                .template(template)
                .build();
    }

}
