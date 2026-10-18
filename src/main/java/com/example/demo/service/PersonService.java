package com.example.demo.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.dto.NameRequest;
import com.example.demo.dto.PersonResponse;
import com.example.demo.repository.PersonRepository;

@Service
public class PersonService {

	private final PersonRepository repository;

	public PersonService(PersonRepository repository) {
		this.repository = repository;
	}

	public PersonResponse addPerson(NameRequest request) {
		if (request.name() == null || request.name().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
		}
		return PersonResponse.from(repository.insertPerson(request.name()));
	}

	public PersonResponse getPerson(Long id) {
		return repository.findPersonById(id)
				.map(PersonResponse::from)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "person not found"));
	}

}
