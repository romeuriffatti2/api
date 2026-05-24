package com.example.cert.event;

import com.example.cert.domain.Usuario;
import org.springframework.context.ApplicationEvent;

/**
 * Evento de domínio publicado sempre que um novo {@link Usuario} é criado com sucesso no sistema.
 *
 * <p>Este evento segue o padrão Observer via {@code ApplicationEvent} do Spring Framework,
 * permitindo que qualquer componente interessado reaja à criação de um usuário sem que o
 * publicador (ex.: {@code UserService} ou {@code AdminSeeder}) precise conhecer os ouvintes.
 *
 * <p>Benefícios arquiteturais:
 * <ul>
 *   <li><b>SRP:</b> O serviço de usuários não precisa mais orquestrar efeitos colaterais como
 *       clonagem de templates.</li>
 *   <li><b>OCP:</b> Novos comportamentos pós-criação (ex.: envio de e-mail, log de auditoria)
 *       podem ser adicionados criando novos {@code @EventListener}, sem alterar código existente.</li>
 * </ul>
 *
 * @see com.example.cert.listener.UserTemplateListener
 */
public class UserCreatedEvent extends ApplicationEvent {

    private final Usuario usuario;

    /**
     * Constrói o evento carregando o usuário recém-criado.
     *
     * @param source  O objeto que publicou o evento (geralmente {@code this}).
     * @param usuario O {@link Usuario} que foi persistido com sucesso.
     */
    public UserCreatedEvent(Object source, Usuario usuario) {
        super(source);
        this.usuario = usuario;
    }

    /**
     * Retorna o usuário associado a este evento.
     *
     * @return O {@link Usuario} criado.
     */
    public Usuario getUsuario() {
        return usuario;
    }
}
