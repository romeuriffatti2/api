package com.example.cert.request;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CertificateItemRequest {
    private String name;
    private String cpf;
    private String email;
    private Long personId;
    private UUID validationCode;
    private String pdfBase64;
    private java.util.Map<String, Object> metadata;
}
