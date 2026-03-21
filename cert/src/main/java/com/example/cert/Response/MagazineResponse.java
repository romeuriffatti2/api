package com.example.cert.Response;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MagazineResponse {

    private Long id;

    private String name;

    private String isbn;

    private String issn;

    private String email;
}