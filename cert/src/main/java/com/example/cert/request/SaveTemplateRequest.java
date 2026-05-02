package com.example.cert.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SaveTemplateRequest {

    private String name;

    private String issuerName;

    /** JSON serializado do PDFME: { basePdf: "base64...", schemas: [...] } */
    private String jsonSchema;
}
