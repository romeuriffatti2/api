package com.example.cert.request;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class RegisterRequest {
    private String name;
    private String cpf;
    private String email;
    private String password;
    private LocalDate birthDate;
}
