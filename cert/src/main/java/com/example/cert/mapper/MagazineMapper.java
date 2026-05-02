package com.example.cert.mapper;

import com.example.cert.Response.MagazineResponse;
import com.example.cert.domain.Magazine;
import com.example.cert.request.MagazineRequest;

import org.springframework.stereotype.Component;

@Component
public class MagazineMapper {

    public static MagazineResponse toResponse(Magazine magazine) {

        return MagazineResponse.builder()
                .id(magazine.getId())
                .name(magazine.getName())
                .issn(magazine.getIssn())
                .email(magazine.getEmail())
                .cnpj(magazine.getCnpj())
                .build();
    }

    public static Magazine toEntity(MagazineRequest request) {
        return Magazine.builder()
                .name(request.getName())
                .issn(request.getIssn())
                .email(request.getEmail())
                .cnpj(request.getCnpj())
                .build();
    }
}