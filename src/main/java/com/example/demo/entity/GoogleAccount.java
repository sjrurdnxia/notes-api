package com.example.demo.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Links a user to their Google account, and holds what's needed to read their calendar later
@Entity
@Table(name = "google_accounts")
public class GoogleAccount {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false, unique = true)
	private Long userId;

	// Google's permanent id for the account (the email can change, this can't)
	@Column(name = "google_sub", nullable = false, unique = true)
	private String googleSub;

	private String email;

	// Encrypted; lets the backend get fresh calendar access without the user signing in again
	@Column(name = "refresh_token", columnDefinition = "TEXT")
	private String refreshToken;

	@Column(name = "calendar_granted", nullable = false)
	private boolean calendarGranted;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected GoogleAccount() {
	}

	public Long getId() {
		return id;
	}

	public Long getUserId() {
		return userId;
	}

	public String getGoogleSub() {
		return googleSub;
	}

	public String getEmail() {
		return email;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public boolean isCalendarGranted() {
		return calendarGranted;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
