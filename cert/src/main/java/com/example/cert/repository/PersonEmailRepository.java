package com.example.cert.repository;

import com.example.cert.domain.PersonEmail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PersonEmailRepository extends JpaRepository<PersonEmail, Long> {

    /**
     * Busca o registro de e-mail pelo endereço, independente do status (ACTIVE ou INACTIVE).
     * <p>
     * Usado para localizar a Person associada ao e-mail informado na funcionalidade
     * "Receber por E-mail", permitindo que e-mails antigos (inativos) também sejam resolvidos.
     */
    Optional<PersonEmail> findByEmail(String email);

    /**
     * Verifica se o e-mail já existe na tabela, independente do status.
     * <p>
     * Usado para garantir unicidade global: nenhum e-mail pode ser atribuído
     * a uma segunda pessoa, mesmo que esteja inativo na pessoa original.
     */
    boolean existsByEmail(String email);
}
