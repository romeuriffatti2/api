package com.example.cert.Exceptions;

import lombok.Builder;
import lombok.Getter;

/**
 * Body de resposta para o caso {@link PersonDeletedException}.
 * Carrega o {@code errorCode} que o frontend usa para identificar
 * que deve oferecer a opção de reativação.
 */
@Getter
@Builder
public class PersonDeletedErrorResponse {
    private String errorCode;
    private Long personId;
    private String personName;
    private String message;
}
