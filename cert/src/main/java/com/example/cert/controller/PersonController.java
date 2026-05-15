package com.example.cert.controller;

import com.example.cert.Response.PersonResponse;
import com.example.cert.request.PersonRequest;
import com.example.cert.service.PersonService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/persons")
@AllArgsConstructor
public class PersonController {

    private final PersonService personService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PersonResponse postPerson(@RequestBody @Valid PersonRequest personRequest) {
        return personService.postPerson(personRequest);
    }

    @GetMapping
    public java.util.List<PersonResponse> getAllPersons() {
        return personService.getAllPersons();
    }
}
