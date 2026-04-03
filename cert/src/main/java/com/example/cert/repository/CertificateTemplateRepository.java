package com.example.cert.repository;

import com.example.cert.domain.CertificateTemplate;
import com.example.cert.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CertificateTemplateRepository extends JpaRepository<CertificateTemplate, Long> {

    /** Retorna todos os templates padrão do sistema (systemDefault=true, owner=null) */
    List<CertificateTemplate> findBySystemDefaultTrue();

    /** Retorna todos os templates de um usuário específico */
    List<CertificateTemplate> findByOwner(Usuario owner);

    /** Busca um template pelo ID garantindo que pertence ao owner — usado em operações de edição/reset */
    Optional<CertificateTemplate> findByIdAndOwner(Long id, Usuario owner);

    /** Busca templates de um usuário por tipo */
    Optional<CertificateTemplate> findByOwnerAndType(Usuario owner, String type);
}
