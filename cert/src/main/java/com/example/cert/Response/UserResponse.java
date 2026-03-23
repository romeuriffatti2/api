package com.example.cert.Response;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String name;
    private String cpf;
    private String email;
    private LocalDate birthDate;
    private LocalDateTime createdAt;
}
