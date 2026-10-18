package com.example.demo.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import com.example.demo.entity.User;

// Issues the app's own login tokens (sent as "Authorization: Bearer ..." on every API call)
@Service
public class TokenService {

	private final JwtEncoder jwtEncoder;
	private final long expiryMinutes;

	public TokenService(JwtEncoder jwtEncoder, @Value("${jwt.expiry-minutes}") long expiryMinutes) {
		this.jwtEncoder = jwtEncoder;
		this.expiryMinutes = expiryMinutes;
	}

	public String createToken(User user) {
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

}
