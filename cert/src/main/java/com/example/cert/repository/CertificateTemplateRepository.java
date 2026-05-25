package com.example.cert.repository;

import com.example.cert.domain.CertificateTemplate;
import com.example.cert.domain.Magazine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CertificateTemplateRepository extends JpaRepository<CertificateTemplate, Long> {

    /** Retorna todos os templates padrão do sistema (systemDefault=true) */
    List<CertificateTemplate> findBySystemDefaultTrue();

    /** Retorna todos os templates de uma revista específica */
    List<CertificateTemplate> findByMagazine(Magazine magazine);

    /** Busca um template pelo ID garantindo que pertence à revista */
    Optional<CertificateTemplate> findByIdAndMagazine(Long id, Magazine magazine);

    /** Busca templates de uma revista por tipo */
    Optional<CertificateTemplate> findByMagazineAndType(Magazine magazine, String type);
}
