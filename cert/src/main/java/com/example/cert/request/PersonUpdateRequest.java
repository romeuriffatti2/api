package com.example.cert.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.br.CPF;

/**
 * Request para edição e reativação de uma {@link com.example.cert.domain.Person}.
 * <p>
 * Diferente de {@link PersonRequest} (usado no cadastro), aqui o CPF é permitido
 * ser alterado — o vínculo com certificados é por {@code person_id}, não por CPF.
 * O CPF é opcional: pode ser omitido ou atualizado para null.
 */
@Getter
@Setter
public class PersonUpdateRequest {

    @NotBlank(message = "Nome é obrigatório")
    private String name;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    private String email;

    /**
     * CPF é opcional. Quando informado, deve ser um CPF válido.
     */
    @CPF(message = "CPF inválido")
    private String cpf;
}
