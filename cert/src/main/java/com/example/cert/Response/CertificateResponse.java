package com.example.cert.Response;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CertificateResponse {

    private Long id;

    private String name;

    private UUID validationCode;

    private MagazineResponse magazineResponse;

    private String volume;

    private String number;

    private String type;

    private java.util.Map<String, Object> metadata;

    private IssuerResponse issuer;

    private String createdAt;

}
