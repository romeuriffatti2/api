package com.example.cert.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.br.CPF;

@Getter
@Setter
public class PersonRequest {

    @NotBlank(message = "Nome é obrigatório")
    private String name;

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    private String email;

    /**
     * CPF é opcional. Quando informado, deve ser um CPF válido.
     * Não utilizar @NotBlank para permitir omissão.
     */
    @CPF(message = "CPF inválido")
    private String cpf;
}
