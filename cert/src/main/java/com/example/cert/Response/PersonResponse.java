package com.example.cert.Response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PersonResponse {

    private Long id;

    private String name;

    private String email;

    private String cpf;
}
