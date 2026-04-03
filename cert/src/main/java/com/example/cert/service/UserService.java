package com.example.cert.service;

import com.example.cert.Exceptions.UserAlreadyExistsException;
import com.example.cert.Response.UserResponse;
import com.example.cert.domain.CertificateTemplate;
import com.example.cert.domain.UserRole;
import com.example.cert.domain.Usuario;
import com.example.cert.mapper.UserMapper;
import com.example.cert.repository.CertificateTemplateRepository;
import com.example.cert.repository.UserRepository;
import com.example.cert.request.RegisterRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class UserService {

    private UserRepository userRepository;
    private UserMapper userMapper;
    private PasswordEncoder passwordEncoder;
    private CertificateTemplateRepository templateRepository;

    /**
     * Registra um novo usuário. Se a role for CLIENT, clona automaticamente
     * todos os templates padrão do sistema para o novo usuário (onboarding).
     */
    @Transactional
    public UserResponse registerUser(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException("Usuário com e-mail " + request.getEmail() + " já cadastrado.");
        }

        Usuario user = UserMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        Usuario savedUser = userRepository.save(user);

        // Clona os templates padrão do sistema para o novo usuário CLIENT
        if (savedUser.getRole() == UserRole.CLIENT) {
            cloneSystemTemplatesForUser(savedUser);
        }

        return userMapper.toResponse(savedUser);
    }

    /**
     * Clona todos os templates padrão do sistema (systemDefault=true) para o usuário dado.
     * Cada cópia registra o sourceTemplateId, permitindo reset futuro ao padrão.
     */
    private void cloneSystemTemplatesForUser(Usuario newUser) {
        List<CertificateTemplate> systemDefaults = templateRepository.findBySystemDefaultTrue();

        if (systemDefaults.isEmpty()) {
            log.warn("Nenhum template padrão do sistema encontrado durante o onboarding do usuário {}. " +
                    "Execute o DataInitializer primeiro.", newUser.getEmail());
            return;
        }

        systemDefaults.forEach(template -> {
            CertificateTemplate userCopy = CertificateTemplate.builder()
                    .name(template.getName())
                    .type(template.getType())
                    .jsonSchema(template.getJsonSchema())
                    .systemDefault(false)
                    .active(true)
                    .sourceTemplateId(template.getId())  // referência para reset
                    .owner(newUser)
                    .build();
            templateRepository.save(userCopy);
        });

        log.info("Clonados {} templates para o novo usuário {}.", systemDefaults.size(), newUser.getEmail());
    }
}
