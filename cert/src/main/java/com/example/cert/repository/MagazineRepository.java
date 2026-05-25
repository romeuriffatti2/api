package com.example.cert.repository;

import com.example.cert.domain.Magazine;
import com.example.cert.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MagazineRepository extends JpaRepository<Magazine, Long> {
    List<Magazine> findByOwner(Usuario owner);
    Optional<Magazine> findByIdAndOwner(Long id, Usuario owner);
}
