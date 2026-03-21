package com.example.cert.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MagazineRequest {

    private Long id;

    private String name;

    private String isbn;

    private String issn;

    private String email;
}
