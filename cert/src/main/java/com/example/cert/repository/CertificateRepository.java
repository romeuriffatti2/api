package com.example.cert.repository;

import com.example.cert.domain.Certificate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    @EntityGraph(attributePaths = {"magazine"})
    Page<Certificate> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"magazine"})
    Optional<Certificate> findByValidationCode(UUID validationCode);
}
