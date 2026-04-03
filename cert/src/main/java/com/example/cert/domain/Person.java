package com.example.cert.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.validator.constraints.br.CPF;

/**
 * Destinatário de um certificado.
 * Representa a pessoa física que recebe o certificado.
 * O e-mail é único por pessoa e é usado para busca pública de certificados.
 */
@Entity
@Table(name = "person")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String email;

    @CPF
    @Column(unique = true)
    private String cpf;
}
