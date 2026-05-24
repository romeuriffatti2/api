package com.example.cert.listener;

import com.example.cert.domain.UserRole;
import com.example.cert.domain.Usuario;
import com.example.cert.event.UserCreatedEvent;
import com.example.cert.service.templates.TemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ouvinte de eventos responsável por clonar os templates do sistema para um novo usuário.
 *
 * <p>Este componente reage ao {@link UserCreatedEvent}, publicado sempre que um usuário é
 * criado — seja via {@code UserService} (registro normal) ou via {@code AdminSeeder}
 * (boot da aplicação). Desta forma, a lógica de clonagem de templates é completamente
 * desacoplada do fluxo de criação de usuários.
 *
 * <p>Princípios SOLID aplicados:
 * <ul>
 *   <li><b>SRP (Single Responsibility Principle):</b> Esta classe tem uma única responsabilidade:
 *       reagir à criação de usuários e clonar os templates. O {@code UserService} e o
 *       {@code AdminSeeder} não precisam conhecer este comportamento.</li>
 *   <li><b>OCP (Open/Closed Principle):</b> Para adicionar um novo efeito colateral
 *       pós-criação de usuário (ex.: envio de e-mail de boas-vindas), basta criar um novo
 *       {@code @EventListener}. Nenhuma classe existente precisa ser alterada.</li>
 * </ul>
 *
 * @see UserCreatedEvent
 * @see TemplateService
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserTemplateListener {

    private final TemplateService templateService;

    /**
     * Escuta o {@link UserCreatedEvent} e clona os templates do sistema para o novo usuário,
     * caso ele tenha o papel (role) de {@code CLIENT} ou {@code ADMIN}.
     *
     * <p>Este método é executado dentro da mesma transação do publicador do evento,
     * garantindo atomicidade entre a criação do usuário e a clonagem dos seus templates.
     *
     * @param event O evento contendo o {@link Usuario} recém-criado.
     */
    @Transactional
    @EventListener
    public void onUserCreated(UserCreatedEvent event) {
        Usuario usuario = event.getUsuario();

        if (usuario.getRole() == UserRole.CLIENT || usuario.getRole() == UserRole.ADMIN) {
            log.info("UserTemplateListener: clonando templates para o usuário '{}' com role '{}'.",
                    usuario.getEmail(), usuario.getRole());
            templateService.cloneTemplatesForUser(usuario);
        }
    }
}
