package com.example.demo.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;

@Configuration
public class AnthropicConfig {

	// The key comes from secrets.properties, or the ANTHROPIC_API_KEY environment variable
	@Bean
	AnthropicClient anthropicClient(@Value("${anthropic.api-key}") String apiKey) {
		if (apiKey.isBlank()) {
			throw new IllegalStateException(
					"No Claude API key: set anthropic.api-key in secrets.properties or the ANTHROPIC_API_KEY environment variable");
		}
		return AnthropicOkHttpClient.builder()
				.apiKey(apiKey)
				// A suggestion that arrives this late is stale anyway, since the user has kept typing
				.timeout(Duration.ofSeconds(30))
				.build();
	}

}
