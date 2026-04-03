package com.example.cert.mapper;

import com.example.cert.Response.UserResponse;
import com.example.cert.domain.UserRole;
import com.example.cert.domain.Usuario;
import com.example.cert.request.RegisterRequest;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(Usuario user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .cpf(user.getCpf())
                .email(user.getEmail())
                .birthDate(user.getBirthDate())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public static Usuario toEntity(RegisterRequest request) {
        UserRole role = request.getRole() != null ? request.getRole() : UserRole.CLIENT;
        return Usuario.builder()
                .name(request.getName())
                .cpf(request.getCpf())
                .email(request.getEmail())
                .password(request.getPassword())
                .birthDate(request.getBirthDate())
                .role(role)
                .build();
    }
}

