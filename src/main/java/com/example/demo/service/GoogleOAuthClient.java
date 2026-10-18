package com.example.demo.service;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// The calls to Google shared by "Sign in with Google" and "Connect Google Calendar"
@Component
public class GoogleOAuthClient {

	private static final Logger log = LoggerFactory.getLogger(GoogleOAuthClient.class);

	private static final String AUTHORIZE_URL = "https://accounts.google.com/o/oauth2/v2/auth";
	private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
	private static final String USERINFO_URL = "https://openidconnect.googleapis.com/v1/userinfo";

	public static final String CALENDAR_SCOPE = "https://www.googleapis.com/auth/calendar.readonly";

	// Google's responses (snake_case JSON)
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record GoogleTokens(@JsonProperty("access_token") String accessToken,
			@JsonProperty("refresh_token") String refreshToken, String scope) {

		// The user can untick calendar access on Google's consent screen, so check what was granted
		public boolean grantedCalendar() {
			return scope != null && Arrays.asList(scope.split(" ")).contains(CALENDAR_SCOPE);
		}

	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record GoogleUserInfo(String sub, String email, @JsonProperty("email_verified") Boolean emailVerified) {
	}

	private final RestClient restClient = RestClient.create();
	private final String clientId;
	private final String clientSecret;

	public GoogleOAuthClient(@Value("${google.client-id}") String clientId,
			@Value("${google.client-secret}") String clientSecret) {
		this.clientId = clientId;
		this.clientSecret = clientSecret;
	}

	public String authorizationUrl(String scopes, String redirectUri, String state) {
		requireConfigured();
		return UriComponentsBuilder.fromUriString(AUTHORIZE_URL)
				.queryParam("client_id", clientId)
				.queryParam("redirect_uri", redirectUri)
				.queryParam("response_type", "code")
				.queryParam("scope", scopes)
				// offline + consent makes Google return a refresh token, so the calendar can be read later
				.queryParam("access_type", "offline")
				.queryParam("prompt", "consent")
				.queryParam("include_granted_scopes", "true")
				.queryParam("state", state)
				.encode()
				.build()
				.toUriString();
	}

	// redirectUri must be the same one used in authorizationUrl, or Google rejects the code
	public GoogleTokens exchangeCode(String code, String redirectUri) {
		requireConfigured();
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("code", code);
		form.add("client_id", clientId);
		form.add("client_secret", clientSecret);
		form.add("redirect_uri", redirectUri);
		form.add("grant_type", "authorization_code");
		try {
			return restClient.post()
					.uri(TOKEN_URL)
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.body(form)
					.retrieve()
					.body(GoogleTokens.class);
		}
		catch (RestClientException e) {
			log.warn("Google token exchange failed", e);
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
					"Google rejected the sign-in code (it may have expired or been used already), please start again");
		}
	}

	// Returns the account details, and rejects accounts whose email Google hasn't verified
	public GoogleUserInfo fetchVerifiedUser(String accessToken) {
		GoogleUserInfo info;
		try {
			info = restClient.get()
					.uri(USERINFO_URL)
					.headers(h -> h.setBearerAuth(accessToken))
					.retrieve()
					.body(GoogleUserInfo.class);
		}
		catch (RestClientException e) {
			log.warn("Google userinfo request failed", e);
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "could not read Google account details");
		}
		if (info == null || !Boolean.TRUE.equals(info.emailVerified())) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google account email is not verified");
		}
		return info;
	}

	public void requireConfigured() {
		if (clientId.isBlank() || clientSecret.isBlank()) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
					"Google is not configured: set google.client-id and google.client-secret in secrets.properties");
		}
	}

}
