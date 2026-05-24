package com.example.cert.service;

import com.example.cert.Exceptions.UserAlreadyExistsException;
import com.example.cert.Response.UserResponse;
import com.example.cert.domain.Usuario;
import com.example.cert.event.UserCreatedEvent;
import com.example.cert.mapper.UserMapper;
import com.example.cert.repository.UserRepository;
import com.example.cert.request.RegisterRequest;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço responsável pelas operações de negócio relacionadas aos usuários da aplicação.
 *
 * <p>Esta classe segue o <b>Single Responsibility Principle (SRP)</b>: sua única
 * responsabilidade é orquestrar as regras de negócio da criação de usuários. Efeitos
 * colaterais como inicialização de templates, envio de e-mails ou auditoria são delegados
 * a ouvintes de eventos via {@link ApplicationEventPublisher}, seguindo o
 * <b>Open/Closed Principle (OCP)</b>: novos comportamentos pós-criação podem ser adicionados
 * sem alterar esta classe.
 *
 * <p>Após persistir um novo usuário com sucesso, este serviço publica um
 * {@link UserCreatedEvent}, que é então tratado pelos respectivos {@code @EventListener}
 * registrados no contexto do Spring.
 *
 * @see UserCreatedEvent
 * @see com.example.cert.listener.UserTemplateListener
 */
@Service
@AllArgsConstructor
@Slf4j
public class UserService {

    private UserRepository userRepository;
    private UserMapper userMapper;
    private PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Registra um novo usuário na aplicação.
     *
     * <p>O fluxo de execução é:
     * <ol>
     *   <li>Valida se o e-mail já está em uso.</li>
     *   <li>Converte o {@link RegisterRequest} para a entidade {@link Usuario}.</li>
     *   <li>Codifica a senha com {@link PasswordEncoder}.</li>
     *   <li>Persiste o usuário no banco de dados.</li>
     *   <li>Publica o {@link UserCreatedEvent}, notificando os ouvintes registrados
     *       (ex.: clonagem de templates).</li>
     * </ol>
     *
     * @param request O DTO contendo os dados de registro do novo usuário.
     * @return {@link UserResponse} com os dados do usuário criado.
     * @throws UserAlreadyExistsException Se o e-mail informado já estiver cadastrado.
     */
    @Transactional
    public UserResponse registerUser(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException("Usuário com e-mail " + request.getEmail() + " já cadastrado.");
        }

        Usuario user = UserMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        Usuario savedUser = userRepository.save(user);
        log.info("UserService: usuário '{}' criado com sucesso.", savedUser.getEmail());

        eventPublisher.publishEvent(new UserCreatedEvent(this, savedUser));

        return userMapper.toResponse(savedUser);
    }

}
