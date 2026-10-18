package com.example.demo.config;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.AesGcmBytesEncryptor;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.crypto.encrypt.TextEncryptor;

@Configuration
public class EncryptionConfig {

	// Encrypts tokens stored in the database (AES-256-GCM); output is Base64 text
	@Bean
	TextEncryptor tokenEncryptor(@Value("${token-encryption.password}") String password,
			@Value("${token-encryption.salt}") String salt) {
		if (password.isBlank() || salt.isBlank()) {
			throw new IllegalStateException(
					"Set token-encryption.password and token-encryption.salt (hex) in secrets.properties");
		}
		BytesEncryptor bytes = AesGcmBytesEncryptor.withPassword(password, salt).build();
		return new TextEncryptor() {

			@Override
			public String encrypt(String text) {
				return Base64.getEncoder().encodeToString(bytes.encrypt(text.getBytes(StandardCharsets.UTF_8)));
			}

			@Override
			public String decrypt(String encryptedText) {
				return new String(bytes.decrypt(Base64.getDecoder().decode(encryptedText)), StandardCharsets.UTF_8);
			}

		};
	}

}
