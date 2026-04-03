package com.example.cert.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "validation_code")
    private UUID validationCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "magazine_id", nullable = false)
    private Magazine magazine;

    @CreationTimestamp
    private java.time.LocalDateTime createdAt;

    @Column(name = "volume")
    private String volume;

    @Column(name = "number")
    private String number;

    @Column(name = "type")
    private String type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issuer_id")
    private Issuer issuer;

    /**
     * Destinatário do certificado. Substituirá gradualmente o campo "name" legado.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id")
    private Person person;

    /**
     * E-mail do destinatário, copiado para facilitar o envio sem JOIN na Person.
     */
    private String recipientEmail;

    /**
     * Status do ciclo de vida do certificado em relação ao envio de e-mail.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private CertificateStatus status = CertificateStatus.GENERATED;

    /**
     * Template utilizado para gerar este certificado.
     * Mantém rastreabilidade histórica do design usado.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private CertificateTemplate template;

}
