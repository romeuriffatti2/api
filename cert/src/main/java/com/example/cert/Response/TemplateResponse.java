package com.example.cert.Response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TemplateResponse {

    private Long id;
    private String name;
    private String issuerName;
    private String type;
    private boolean systemDefault;
    private Long sourceTemplateId;
    private String jsonSchema;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
