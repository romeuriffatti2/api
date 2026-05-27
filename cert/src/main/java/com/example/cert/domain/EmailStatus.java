package com.example.cert.domain;

/**
 * Status de um e-mail registrado em {@link PersonEmail}.
 * <p>
 * Um e-mail é {@code ACTIVE} enquanto for o endereço atual da pessoa.
 * Ao ser substituído por um novo endereço, passa para {@code INACTIVE} (delete lógico),
 * mantendo o histórico para permitir buscas retroativas de certificados.
 */
public enum EmailStatus {
    ACTIVE,
    INACTIVE
}
