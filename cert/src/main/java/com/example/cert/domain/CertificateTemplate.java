package com.example.cert.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Template de certificado pertencente a um usuário.
 * - Templates com systemDefault=true e owner=null são os padrões imutáveis do sistema.
 * - Templates de usuários são clonados dos padrões no onboarding e têm owner = o usuário.
 * - sourceTemplateId guarda o ID do template padrão de origem, permitindo reset.
 */
@Entity
@Table(name = "certificate_template")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /**
     * Tipo do certificado: "participacao", "publicacao", "parecerista",
     * "corpo-editorial", "dossie", "aceite", "custom"
     */
    @Column(nullable = false)
    private String type;

    /**
     * true = template padrão do sistema (imutável pelos clientes).
     * Apenas o ADMIN pode editar templates do sistema.
     */
    @Builder.Default
    private boolean systemDefault = false;

    @Builder.Default
    private boolean active = true;

    @Column(name = "issuer_name")
    private String issuerName;

    /**
     * JSON Schema do PDFME: { basePdf: "base64...", schemas: [...] }
     * Armazena o design visual completo do certificado.
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String jsonSchema;

    /**
     * Dono do template. null = template padrão do sistema.
     * Para templates de usuários, aponta para o Usuario proprietário.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = true)
    private Usuario owner;

    /**
     * ID do template padrão do sistema que originou este template por clonagem.
     * Permite ao usuário resetar o template para o estado original do sistema.
     * null = template criado do zero pelo usuário (sem origem padrão).
     */
    @Column(name = "source_template_id")
    private Long sourceTemplateId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
