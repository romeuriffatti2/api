package com.example.cert.service;

import com.example.cert.Response.PersonResponse;
import com.example.cert.domain.EmailStatus;
import com.example.cert.domain.Person;
import com.example.cert.domain.PersonEmail;
import com.example.cert.mapper.PersonMapper;
import com.example.cert.repository.PersonEmailRepository;
import com.example.cert.repository.PersonRepository;
import com.example.cert.request.PersonRequest;
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
     * Valida unicidade de e-mail e CPF. A checagem de e-mail é feita tanto na tabela
     * {@code person} (e-mail ativo) quanto na tabela {@code person_email} (histórico),
     * garantindo que nenhum e-mail — mesmo que inativo em outra pessoa — seja reutilizado.
     */
    @Transactional
    public PersonResponse postPerson(PersonRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        // Verifica unicidade de e-mail: inclui inativos em person_email
        if (personEmailRepository.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email já cadastrado");
        }

        if (personRepository.findByCpf(request.getCpf()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CPF já cadastrado");
        }

        Person person = PersonMapper.toEntity(request);
        person.setEmail(normalizedEmail);
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

    public List<PersonResponse> getAllPersons() {
        return personRepository.findAll().stream()
                .map(PersonMapper::toResponse)
                .collect(Collectors.toList());
    }
}
