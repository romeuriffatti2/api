package com.example.cert.domain;

public enum CertificateStatus {
    GENERATED,    // Gerado e salvo no banco com sucesso
    EMAIL_SENT,   // E-mail com o PDF enviado com sucesso ao destinatário
    EMAIL_FAILED  // Falha no envio do e-mail (pode ser reprocessado)
}
