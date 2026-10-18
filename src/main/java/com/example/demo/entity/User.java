package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// The Google account email (users sign in with Google only)
	@Column(nullable = false, unique = true)
	private String username;

	protected User() {
	}

	public Long getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

}
