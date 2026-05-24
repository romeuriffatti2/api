package com.example.cert.config;

import com.example.cert.domain.UserRole;
import com.example.cert.domain.Usuario;
import com.example.cert.event.UserCreatedEvent;
import com.example.cert.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeder responsável por garantir que um usuário administrador exista no banco de dados
 * ao iniciar a aplicação.
 *
 * <p>Este componente implementa {@link ApplicationRunner} e é executado automaticamente
 * pelo Spring Boot na inicialização. A anotação {@link Order}(2) assegura que ele seja
 * executado <b>após</b> o {@link TemplateSeeder}, garantindo que os templates base já
 * estejam disponíveis no banco quando o admin for criado e seu evento de clonagem for
 * processado pelo {@code UserTemplateListener}.
 *
 * <p>Ao criar o usuário admin, este seeder publica um {@link UserCreatedEvent}, participando
 * do mesmo fluxo de eventos que o {@code UserService} — garantindo consistência no estado
 * da aplicação (o admin também receberá seus templates clonados automaticamente).
 *
 * <p>Princípios SOLID aplicados:
 * <ul>
 *   <li><b>SRP:</b> Responsabilidade única de verificar e criar o usuário administrador
 *       inicial.</li>
 *   <li><b>OCP:</b> Ao disparar o evento, beneficia-se de todos os ouvintes registrados
 *       sem precisar conhecê-los diretamente.</li>
 * </ul>
 *
 * @see TemplateSeeder
 * @see UserCreatedEvent
 * @see com.example.cert.listener.UserTemplateListener
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class AdminSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${admin.seed.email}")
    private String adminEmail;

    @Value("${admin.seed.password}")
    private String adminPassword;

    @Value("${admin.seed.name}")
    private String adminName;

    /**
     * Verifica se um usuário ADMIN já existe e, caso contrário, cria um com as credenciais
     * definidas nas propriedades da aplicação ({@code admin.seed.*}).
     *
     * <p>Após persistir o admin, publica um {@link UserCreatedEvent} para que os ouvintes
     * (como o {@code UserTemplateListener}) possam executar os efeitos colaterais necessários,
     * como a clonagem de templates.
     *
     * @param args Os argumentos de inicialização da aplicação (não utilizados).
     */
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

        eventPublisher.publishEvent(new UserCreatedEvent(this, admin));
        log.info("AdminSeeder: UserCreatedEvent publicado para o admin '{}'.", adminEmail);
    }
}
