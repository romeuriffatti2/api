package com.example.cert.Response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class JwtResponse {
    private String token;
    private String type = "Bearer";
    private String email;

    public JwtResponse(String accessToken, String email) {
        this.token = accessToken;
        this.email = email;
    }
}
