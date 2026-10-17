package org.ahmadi.jevtriage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

@ConfigurationProperties(prefix = "jev")
public record JevProperties(String baseUrl, String model, String apiKey, Duration timeout) {

}