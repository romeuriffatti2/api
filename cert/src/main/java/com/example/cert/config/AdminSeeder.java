package com.example.cert.config;

import com.example.cert.domain.UserRole;
import com.example.cert.domain.Usuario;
import com.example.cert.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.seed.email}")
    private String adminEmail;

    @Value("${admin.seed.password}")
    private String adminPassword;

    @Value("${admin.seed.name}")
    private String adminName;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(UserRole.ADMIN)) {
            log.info("AdminSeeder: usuário ADMIN já existe, nenhuma ação necessária.");
            return;
        }

        Usuario admin = Usuario.builder()
                .name(adminName)
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .role(UserRole.ADMIN)
                .build();

        userRepository.save(admin);
        log.info("AdminSeeder: primeiro usuário ADMIN criado com e-mail '{}'.", adminEmail);
    }
}
