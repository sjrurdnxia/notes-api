package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Person;

public interface PersonRepository extends JpaRepository<Person, Long> {

	// Repository query methods run read-only by default, so writes need @Transactional
	@Transactional
	@Query(value = "INSERT INTO names (name) VALUES (:name) RETURNING *", nativeQuery = true)
	Person insertPerson(@Param("name") String name);

	@Query(value = "SELECT * FROM names WHERE id = :id", nativeQuery = true)
	Optional<Person> findPersonById(@Param("id") Long id);

}
