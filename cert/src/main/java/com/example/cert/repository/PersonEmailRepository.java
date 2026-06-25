package com.example.cert.repository;

import com.example.cert.domain.PersonEmail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PersonEmailRepository extends JpaRepository<PersonEmail, Long> {

    /**
     * Busca o registro de e-mail pelo endereço, no escopo do usuário.
     */
    Optional<PersonEmail> findByEmailAndPersonUsuarioId(String email, Long usuarioId);

    /**
     * Busca os registros de e-mail pelo endereço em todos os usuários (endpoint público).
     */
    List<PersonEmail> findAllByEmail(String email);

    /**
     * Verifica se o e-mail já existe na tabela no escopo do usuário.
     */
    boolean existsByEmailAndPersonUsuarioId(String email, Long usuarioId);
}
