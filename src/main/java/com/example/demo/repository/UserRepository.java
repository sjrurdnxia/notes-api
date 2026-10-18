package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

	// Repository query methods run read-only by default, so writes need @Transactional
	@Transactional
	@Query(value = "INSERT INTO users (username, password_hash) VALUES (:username, :passwordHash) RETURNING *", nativeQuery = true)
	User insertUser(@Param("username") String username, @Param("passwordHash") String passwordHash);

	@Query(value = "SELECT * FROM users WHERE username = :username", nativeQuery = true)
	Optional<User> findUserByUsername(@Param("username") String username);

}
