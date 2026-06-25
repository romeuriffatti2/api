package com.example.cert.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Registro de e-mail de uma {@link Person}, suportando histórico de endereços.
 *
 * <p>A coluna {@code email} possui constraint {@code UNIQUE} global: um mesmo e-mail
 * não pode pertencer a duas pessoas diferentes, mesmo que esteja inativo ({@link EmailStatus#INACTIVE}).
 * Isso garante que a chave de identidade do destinatário nunca seja ambígua.
 *
 * <p>Ao alterar o e-mail de uma pessoa:
 * <ol>
 *   <li>O registro atual é marcado como {@link EmailStatus#INACTIVE} (delete lógico).</li>
 *   <li>Um novo registro com {@link EmailStatus#ACTIVE} é criado para o novo endereço.</li>
 *   <li>{@code Person.email} é atualizado para refletir o e-mail ativo.</li>
 * </ol>
 *
 * <p>Isso permite buscar certificados por e-mail antigo e reenviá-los para o
 * endereço ativo atual da pessoa, sem expor informações desnecessárias (LGPD).
 */
@Entity
@Table(name = "person_email")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PersonEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmailStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
