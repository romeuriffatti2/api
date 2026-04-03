package com.example.cert.Response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JwtResponse {
    private String token;
    private String type = "Bearer";
    private String email;
    private String role;   // role do usuário: "ADMIN" ou "CLIENT"

    public JwtResponse(String accessToken, String email) {
        this.token = accessToken;
        this.email = email;
    }

    public JwtResponse(String accessToken, String email, String role) {
        this.token = accessToken;
        this.email = email;
        this.role = role;
    }
}
