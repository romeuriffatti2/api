package com.example.cert.repository;

import com.example.cert.domain.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PersonRepository extends JpaRepository<Person, Long> {

    Optional<Person> findByEmailAndUsuarioId(String email, Long usuarioId);

    /** Busca por CPF independente do status de deleção — usado para detectar CPF de pessoa deletada. */
    Optional<Person> findByCpfAndUsuarioId(String cpf, Long usuarioId);

    /** Listagem de pessoas ativas para a tela de listagem do usuário. */
    List<Person> findAllByDeletedFalseAndUsuarioId(Long usuarioId);

    /** Busca por ID garantindo que a pessoa não foi deletada e pertence ao usuário. */
    Optional<Person> findByIdAndDeletedFalseAndUsuarioId(Long id, Long usuarioId);

    /** Busca por ID e Usuário. */
    Optional<Person> findByIdAndUsuarioId(Long id, Long usuarioId);

    /** Unicidade de CPF entre pessoas ativas do usuário. */
    Optional<Person> findByCpfAndDeletedFalseAndUsuarioId(String cpf, Long usuarioId);

    /** Unicidade de e-mail entre pessoas ativas do usuário. */
    Optional<Person> findByEmailAndDeletedFalseAndUsuarioId(String email, Long usuarioId);
}
