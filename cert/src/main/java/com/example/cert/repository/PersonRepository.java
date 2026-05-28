package com.example.cert.repository;

import com.example.cert.domain.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PersonRepository extends JpaRepository<Person, Long> {

    Optional<Person> findByEmail(String email);

    /** Busca por CPF independente do status de deleção — usado para detectar CPF de pessoa deletada. */
    Optional<Person> findByCpf(String cpf);

    /** Listagem de pessoas ativas para a tela de listagem. */
    List<Person> findAllByDeletedFalse();

    /** Busca por ID garantindo que a pessoa não foi deletada. */
    Optional<Person> findByIdAndDeletedFalse(Long id);

    /** Unicidade de CPF entre pessoas ativas. */
    Optional<Person> findByCpfAndDeletedFalse(String cpf);

    /** Unicidade de e-mail entre pessoas ativas. */
    Optional<Person> findByEmailAndDeletedFalse(String email);
}
