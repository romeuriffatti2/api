package com.example.cert.service;

import com.example.cert.Exceptions.UserAlreadyExistsException;
import com.example.cert.Response.UserResponse;
import com.example.cert.domain.UserRole;
import com.example.cert.domain.Usuario;
import com.example.cert.mapper.UserMapper;
import com.example.cert.repository.UserRepository;
import com.example.cert.request.RegisterRequest;
import com.example.cert.service.templates.InitializeTemplatesService;
import com.example.cert.service.templates.TemplateService;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class UserService {

    private UserRepository userRepository;
    private UserMapper userMapper;
    private PasswordEncoder passwordEncoder;
    private final InitializeTemplatesService initializeTemplatesService;
    private final TemplateService templateService;

    @Transactional
    public UserResponse registerUser(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException("Usuário com e-mail " + request.getEmail() + " já cadastrado.");
        }

        Usuario user = UserMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        Usuario savedUser = userRepository.save(user);

        initializeTemplatesService.initializeSystemTemplates();

        if (savedUser.getRole() == UserRole.CLIENT || savedUser.getRole() == UserRole.ADMIN) {
            templateService.cloneTemplatesForUser(savedUser);
        }
        return userMapper.toResponse(savedUser);
    }

}
