package com.example.cert.Response;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MagazineResponse {

    private Long id;

    private String name;

    private String issn;

    private String email;

    private String cnpj;
}