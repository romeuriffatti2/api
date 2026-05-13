package com.example.cert.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

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

    @Column(nullable = false)
    private String type;

    @Builder.Default
    private boolean systemDefault = false;

    @Builder.Default
    private boolean active = true;

    @Column(name = "issuer_name")
    private String issuerName;

    @Column(columnDefinition = "TEXT", nullable = false)
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    private String jsonSchema;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = true)
    private Usuario owner;

    @Column(name = "source_template_id")
    private Long sourceTemplateId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
