package com.example.cert.Exceptions;

import lombok.Getter;

/**
 * Lançada quando se tenta cadastrar um CPF que pertence a uma {@link com.example.cert.domain.Person}
 * com delete lógico ativo ({@code deleted = true}).
 * <p>
 * Permite ao frontend oferecer a opção de reativação do cadastro.
 */
@Getter
public class PersonDeletedException extends RuntimeException {

    private final Long personId;
    private final String personName;

    public PersonDeletedException(Long personId, String personName) {
        super("CPF pertence a um cadastro excluído anteriormente.");
        this.personId = personId;
        this.personName = personName;
    }
}
