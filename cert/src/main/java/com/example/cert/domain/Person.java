package com.example.cert.domain;

import jakarta.persistence.*;
import lombok.*;


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

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = true)
    private String cpf;

    @Column(nullable = false)
    private String email;

    @Builder.Default
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean deleted = false;

}
