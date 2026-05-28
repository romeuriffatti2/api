package com.example.cert.service;

import com.example.cert.Exceptions.PersonDeletedException;
import com.example.cert.Response.PersonResponse;
import com.example.cert.domain.EmailStatus;
import com.example.cert.domain.Person;
import com.example.cert.domain.PersonEmail;
import com.example.cert.mapper.PersonMapper;
import com.example.cert.repository.PersonEmailRepository;
import com.example.cert.repository.PersonRepository;
import com.example.cert.request.PersonRequest;
import com.example.cert.request.PersonUpdateRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class PersonService {

    private final PersonRepository personRepository;
    private final PersonEmailRepository personEmailRepository;

    /**
     * Cadastra uma nova Person na plataforma e registra o e-mail no histórico.
     * <p>
     * Valida unicidade de e-mail e CPF apenas entre pessoas <strong>ativas</strong>.
     * Se o CPF pertencer a uma pessoa deletada, retorna 409 com errorCode "PERSON_DELETED"
     * para que o frontend ofereça a opção de reativação.
     */
    @Transactional
    public PersonResponse postPerson(PersonRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        String normalizedCpf = request.getCpf().replaceAll("[^\\d]", "");

        // Verifica unicidade de e-mail: inclui inativos em person_email
        if (personEmailRepository.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email já cadastrado");
        }

        // Verifica CPF apenas entre pessoas ativas
        if (personRepository.findByCpfAndDeletedFalse(normalizedCpf).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CPF já cadastrado");
        }

        // Verifica se CPF pertence a pessoa deletada — oferece reativação no frontend
        personRepository.findByCpf(normalizedCpf).ifPresent(deleted -> {
            if (deleted.isDeleted()) {
                throw new PersonDeletedException(deleted.getId(), deleted.getName());
            }
        });

        Person person = PersonMapper.toEntity(request);
        person.setEmail(normalizedEmail);
        person.setCpf(normalizedCpf);
        personRepository.save(person);

        // Registra o e-mail no histórico como ativo
        PersonEmail personEmail = PersonEmail.builder()
                .person(person)
                .email(normalizedEmail)
                .status(EmailStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
        personEmailRepository.save(personEmail);

        return PersonMapper.toResponse(person);
    }

    /** Lista todas as pessoas ativas (não deletadas). */
    public List<PersonResponse> getAllPersons() {
        return personRepository.findAllByDeletedFalse().stream()
                .map(PersonMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Exclusão lógica de uma Person.
     * <p>
     * Marca {@code deleted = true}. O histórico de {@code PersonEmail} é preservado
     * integralmente para rastreabilidade de certificados emitidos anteriormente.
     */
    @Transactional
    public void deletePerson(Long id) {
        Person person = personRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pessoa não encontrada"));

        person.setDeleted(true);
        personRepository.save(person);
    }

    /**
     * Atualiza os dados de uma Person ativa.
     * <p>
     * CPF e e-mail são validados contra outras pessoas ativas.
     * Se o e-mail for alterado, o registro anterior em {@code PersonEmail} é marcado
     * como {@link EmailStatus#INACTIVE} e um novo registro {@link EmailStatus#ACTIVE} é criado,
     * preservando o histórico completo para resolução retroativa de certificados.
     */
    @Transactional
    public PersonResponse updatePerson(Long id, PersonUpdateRequest request) {
        Person person = personRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pessoa não encontrada"));

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        String normalizedCpf = request.getCpf().replaceAll("[^\\d]", "");

        // Valida CPF: não pode pertencer a outra pessoa ativa
        if (!normalizedCpf.equals(person.getCpf())) {
            personRepository.findByCpfAndDeletedFalse(normalizedCpf).ifPresent(other -> {
                if (!other.getId().equals(id)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "CPF já em uso por outro cadastro");
                }
            });
        }

        // Valida e-mail: não pode pertencer ao histórico de outra pessoa
        if (!normalizedEmail.equals(person.getEmail())) {
            personEmailRepository.findByEmail(normalizedEmail).ifPresent(existing -> {
                if (!existing.getPerson().getId().equals(id)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já em uso por outro cadastro");
                }
            });

            // Inativa e-mail atual no histórico
            personEmailRepository.findByEmail(person.getEmail()).ifPresent(current -> {
                current.setStatus(EmailStatus.INACTIVE);
                personEmailRepository.save(current);
            });

            // Registra novo e-mail como ativo no histórico
            PersonEmail newEmail = PersonEmail.builder()
                    .person(person)
                    .email(normalizedEmail)
                    .status(EmailStatus.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .build();
            personEmailRepository.save(newEmail);

            person.setEmail(normalizedEmail);
        }

        person.setName(request.getName());
        person.setCpf(normalizedCpf);
        personRepository.save(person);

        return PersonMapper.toResponse(person);
    }

    /**
     * Reativa uma Person previamente deletada (delete lógico revertido).
     * <p>
     * Atualiza nome, CPF e e-mail com os dados fornecidos, tratando o histórico
     * de {@code PersonEmail} da mesma forma que {@link #updatePerson}.
     */
    @Transactional
    public PersonResponse reactivatePerson(Long id, PersonUpdateRequest request) {
        Person person = personRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pessoa não encontrada"));

        if (!person.isDeleted()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Pessoa já está ativa");
        }

        String normalizedEmail = request.getEmail().trim().toLowerCase();
        String normalizedCpf = request.getCpf().replaceAll("[^\\d]", "");

        // Valida CPF: não pode pertencer a outra pessoa ativa
        personRepository.findByCpfAndDeletedFalse(normalizedCpf).ifPresent(other -> {
            if (!other.getId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "CPF já em uso por outro cadastro");
            }
        });

        // Valida e trata e-mail
        if (!normalizedEmail.equals(person.getEmail())) {
            personEmailRepository.findByEmail(normalizedEmail).ifPresent(existing -> {
                if (!existing.getPerson().getId().equals(id)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já em uso por outro cadastro");
                }
            });

            // Inativa e-mail anterior no histórico
            personEmailRepository.findByEmail(person.getEmail()).ifPresent(current -> {
                current.setStatus(EmailStatus.INACTIVE);
                personEmailRepository.save(current);
            });

            // Registra novo e-mail ativo
            PersonEmail newEmail = PersonEmail.builder()
                    .person(person)
                    .email(normalizedEmail)
                    .status(EmailStatus.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .build();
            personEmailRepository.save(newEmail);

            person.setEmail(normalizedEmail);
        }

        person.setName(request.getName());
        person.setCpf(normalizedCpf);
        person.setDeleted(false);
        personRepository.save(person);

        return PersonMapper.toResponse(person);
    }
}
