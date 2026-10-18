package com.example.demo.dto;

import com.example.demo.entity.Person;

public record PersonResponse(Long id, String name) {

	public static PersonResponse from(Person person) {
		return new PersonResponse(person.getId(), person.getName());
	}

}
