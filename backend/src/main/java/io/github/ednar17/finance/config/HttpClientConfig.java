package io.github.ednar17.finance.config;

import java.time.Duration;

import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class HttpClientConfig {

    @Bean
    RestClient aiRestClient(AiServiceProperties properties) {
        var settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(5))
                .withReadTimeout(properties.readTimeout());
        return RestClient.builder()
                .baseUrl(properties.url())
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .build();
    }
}
