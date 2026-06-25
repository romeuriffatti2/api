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

    @Transactional
    public PersonResponse postPerson(PersonRequest request, Long usuarioId) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        String normalizedCpf = null;
        if (request.getCpf() != null && !request.getCpf().isBlank()) {
            normalizedCpf = request.getCpf().replaceAll("[^\\d]", "");
        }

        if (normalizedCpf != null) {
            final String cpfToCheck = normalizedCpf;
            personRepository.findByCpfAndUsuarioId(cpfToCheck, usuarioId).ifPresent(personCpf -> {
                if (personCpf.isDeleted()) {
                    throw new PersonDeletedException(personCpf.getId(), personCpf.getName());
                } else {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "CPF já cadastrado na sua lista de pessoas");
                }
            });
        }

        personRepository.findByEmailAndUsuarioId(normalizedEmail, usuarioId).ifPresent(personByEmail -> {
            if (personByEmail.isDeleted()) {
                throw new PersonDeletedException(personByEmail.getId(), personByEmail.getName());
            } else {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email já cadastrado na sua lista de pessoas");
            }
        });

        personEmailRepository.findByEmailAndPersonUsuarioId(normalizedEmail, usuarioId).ifPresent(personEmail -> {
            Person personByEmail = personEmail.getPerson();
            if (personByEmail.isDeleted()) {
                throw new PersonDeletedException(personByEmail.getId(), personByEmail.getName());
            } else {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email já cadastrado na sua lista de pessoas");
            }
        });

        Person person = PersonMapper.toEntity(request);
        person.setEmail(normalizedEmail);
        person.setCpf(normalizedCpf);
        person.setUsuarioId(usuarioId);
        personRepository.save(person);

        PersonEmail personEmail = PersonEmail.builder()
                .person(person)
                .email(normalizedEmail)
                .status(EmailStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
        personEmailRepository.save(personEmail);

        return PersonMapper.toResponse(person);
    }

    public List<PersonResponse> getAllPersons(Long usuarioId) {
        return personRepository.findAllByDeletedFalseAndUsuarioId(usuarioId).stream()
                .map(PersonMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deletePerson(Long id, Long usuarioId) {
        Person person = personRepository.findByIdAndDeletedFalseAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pessoa não encontrada"));

        person.setDeleted(true);
        personRepository.save(person);
    }

    @Transactional
    public PersonResponse updatePerson(Long id, PersonUpdateRequest request, Long usuarioId) {
        Person person = personRepository.findByIdAndDeletedFalseAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pessoa não encontrada"));

        String normalizedEmail = request.getEmail().trim().toLowerCase();

        String normalizedCpf = null;
        if (request.getCpf() != null && !request.getCpf().isBlank()) {
            normalizedCpf = request.getCpf().replaceAll("[^\\d]", "");
        }

        if (normalizedCpf != null && !normalizedCpf.equals(person.getCpf())) {
            final String cpfToCheck = normalizedCpf;
            personRepository.findByCpfAndDeletedFalseAndUsuarioId(cpfToCheck, usuarioId).ifPresent(other -> {
                if (!other.getId().equals(id)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "CPF já em uso por outro cadastro na sua lista");
                }
            });
        }

        if (!normalizedEmail.equals(person.getEmail())) {
            personEmailRepository.findByEmailAndPersonUsuarioId(normalizedEmail, usuarioId).ifPresent(existing -> {
                if (!existing.getPerson().getId().equals(id)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já em uso por outro cadastro na sua lista");
                }
            });

            personEmailRepository.findByEmailAndPersonUsuarioId(person.getEmail(), usuarioId).ifPresent(current -> {
                current.setStatus(EmailStatus.INACTIVE);
                personEmailRepository.save(current);
            });

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

    @Transactional
    public PersonResponse reactivatePerson(Long id, PersonUpdateRequest request, Long usuarioId) {
        Person person = personRepository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pessoa não encontrada"));

        if (!person.isDeleted()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Pessoa já está ativa");
        }

        String normalizedEmail = request.getEmail().trim().toLowerCase();

        String normalizedCpf = null;
        if (request.getCpf() != null && !request.getCpf().isBlank()) {
            normalizedCpf = request.getCpf().replaceAll("[^\\d]", "");
        }

        if (normalizedCpf != null) {
            final String cpfToCheck = normalizedCpf;
            personRepository.findByCpfAndDeletedFalseAndUsuarioId(cpfToCheck, usuarioId).ifPresent(other -> {
                if (!other.getId().equals(id)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "CPF já em uso por outro cadastro na sua lista");
                }
            });
        }

        if (!normalizedEmail.equals(person.getEmail())) {
            personEmailRepository.findByEmailAndPersonUsuarioId(normalizedEmail, usuarioId).ifPresent(existing -> {
                if (!existing.getPerson().getId().equals(id)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já em uso por outro cadastro na sua lista");
                }
            });

            personEmailRepository.findByEmailAndPersonUsuarioId(person.getEmail(), usuarioId).ifPresent(current -> {
                current.setStatus(EmailStatus.INACTIVE);
                personEmailRepository.save(current);
            });

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
