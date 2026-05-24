package com.example.cert.config;

import com.example.cert.service.templates.InitializeTemplatesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Seeder responsável por garantir que os templates base do sistema estejam presentes
 * no banco de dados sempre que a aplicação for inicializada.
 *
 * <p>Este componente implementa {@link ApplicationRunner} e é executado automaticamente
 * pelo Spring Boot na inicialização. Ele isola a responsabilidade de "seed" de dados
 * dos templates, que antes estava equivocadamente acoplada ao fluxo de registro de usuários
 * no {@code UserService}.
 *
 * <p>A anotação {@link Order}(1) garante que este seeder seja executado <b>antes</b> do
 * {@code AdminSeeder}, de modo que os templates já existam no banco quando o administrador
 * for criado e o evento de clonagem for disparado.
 *
 * <p>Princípio SOLID aplicado:
 * <ul>
 *   <li><b>SRP (Single Responsibility Principle):</b> Esta classe tem uma única e clara
 *       responsabilidade: garantir a integridade dos dados base de templates ao iniciar
 *       a aplicação.</li>
 * </ul>
 *
 * @see InitializeTemplatesService
 * @see AdminSeeder
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class TemplateSeeder implements ApplicationRunner {

    private final InitializeTemplatesService initializeTemplatesService;

    /**
     * Executa a inicialização dos templates do sistema durante o boot da aplicação.
     *
     * <p>Chama {@link InitializeTemplatesService#initializeSystemTemplates()} que garante
     * via {@code saveOrUpdate} que os templates padrão existam no banco sem duplicação.
     *
     * @param args Os argumentos de inicialização da aplicação (não utilizados).
     */
    @Override
    public void run(ApplicationArguments args) {
        log.info("TemplateSeeder: inicializando templates base do sistema...");
        initializeTemplatesService.initializeSystemTemplates();
        log.info("TemplateSeeder: templates base inicializados com sucesso.");
    }
}
