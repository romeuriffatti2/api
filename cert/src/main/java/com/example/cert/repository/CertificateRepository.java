package com.example.cert.repository;

import com.example.cert.domain.Certificate;
import com.example.cert.domain.Person;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    @EntityGraph(attributePaths = {"magazine"})
    Page<Certificate> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"magazine"})
    Optional<Certificate> findByValidationCode(UUID validationCode);

    @EntityGraph(attributePaths = {"magazine", "person"})
    List<Certificate> findByPersonEmail(String email);

    /**
     * Busca todos os certificados vinculados diretamente ao objeto Person.
     * Usado no fluxo de reenvio por e-mail quando o e-mail buscado pode ser
     * antigo (inativo) — a Person é resolvida via person_email e os certificados
     * são então recuperados por este método.
     */
    @EntityGraph(attributePaths = {"magazine", "person"})
    List<Certificate> findByPerson(Person person);
}
