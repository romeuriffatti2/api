package com.example.cert.mapper;

import com.example.cert.Response.PersonResponse;
import com.example.cert.domain.Person;
import com.example.cert.request.PersonRequest;
import org.springframework.stereotype.Component;

@Component
public class PersonMapper {

    public static PersonResponse toResponse(Person person) {
        return PersonResponse.builder()
                .id(person.getId())
                .name(person.getName())
                .email(person.getEmail())
                .cpf(person.getCpf())
                .build();
    }

    public static Person toEntity(PersonRequest request) {
        return Person.builder()
                .name(request.getName())
                .email(request.getEmail())
                .cpf(request.getCpf())
                .build();
    }
}
