package com.example.cert.request;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CertificateItemRequest {
    private String name;
    private UUID validationCode;
    private java.util.Map<String, Object> metadata;
}
