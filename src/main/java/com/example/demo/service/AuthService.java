package com.example.demo.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.TokenResponse;
import com.example.demo.dto.UserResponse;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtEncoder jwtEncoder;
	private final long expiryMinutes;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder,
			@Value("${jwt.expiry-minutes}") long expiryMinutes) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtEncoder = jwtEncoder;
		this.expiryMinutes = expiryMinutes;
	}

	public UserResponse register(LoginRequest request) {
		validate(request);
		if (userRepository.findUserByUsername(request.username()).isPresent()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "username already taken");
		}
		String hash = passwordEncoder.encode(request.password());
		return UserResponse.from(userRepository.insertUser(request.username(), hash));
	}

	public TokenResponse login(LoginRequest request) {
		validate(request);
		User user = userRepository.findUserByUsername(request.username())
				.filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid username or password"));
		return new TokenResponse(createToken(user));
	}

	private String createToken(User user) {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer("demo")
				// The user id identifies the caller on every request; username is just for readability
				.subject(String.valueOf(user.getId()))
				.claim("username", user.getUsername())
				.issuedAt(now)
				.expiresAt(now.plus(expiryMinutes, ChronoUnit.MINUTES))
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

	private void validate(LoginRequest request) {
		if (request.username() == null || request.username().isBlank()
				|| request.password() == null || request.password().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username and password are required");
		}
	}

}
