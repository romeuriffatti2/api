package com.example.cert.service;

import com.example.cert.Response.PersonResponse;
import com.example.cert.domain.Person;
import com.example.cert.mapper.PersonMapper;
import com.example.cert.repository.PersonRepository;
import com.example.cert.request.PersonRequest;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@AllArgsConstructor
public class PersonService {

    private final PersonRepository personRepository;

    public PersonResponse postPerson(PersonRequest request) {

        if (personRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email já cadastrado");
        }

        if (personRepository.findByCpf(request.getCpf()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CPF já cadastrado");
        }

        Person person = PersonMapper.toEntity(request);
        personRepository.save(person);

        return PersonMapper.toResponse(person);
    }

    public java.util.List<PersonResponse> getAllPersons() {
        return personRepository.findAll().stream()
                .map(PersonMapper::toResponse)
                .collect(java.util.stream.Collectors.toList());
    }
}
