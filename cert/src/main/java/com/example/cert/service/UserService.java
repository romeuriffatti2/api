package com.example.cert.service;

import com.example.cert.Exceptions.UserAlreadyExistsException;
import com.example.cert.Response.UserResponse;
import com.example.cert.domain.Usuario;
import com.example.cert.mapper.UserMapper;
import com.example.cert.repository.UserRepository;
import com.example.cert.request.RegisterRequest;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserService {

    private UserRepository userRepository;
    private UserMapper userMapper;
    private PasswordEncoder passwordEncoder;

    public UserResponse registerUser(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException("Usuário com e-mail " + request.getEmail() + " já cadastrado.");
        }

        Usuario user = UserMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        Usuario savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }
}
