package org.ahmadi.jevtriage.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import java.net.http.HttpClient;

@Configuration
public class JevConfig {

    @Bean
    RestClient jevRestClient(JevProperties props) {
        var httpClient = HttpClient.newBuilder()
                .connectTimeout(props.timeout())
                .build();
        var factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(props.timeout());

        return RestClient.builder()
                .baseUrl(props.baseUrl())
                .defaultHeader("Authorization", "Bearer " + props.apiKey())
                .requestFactory(factory)
                .build();
    }
}