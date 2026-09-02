package ru.zdadco.indexer.api.config;

import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.net.http.HttpClient;

@Configuration
public class InsecureSslConfiguration {

    @Bean
    RestClientCustomizer insecureRestClientCustomizer() {
        HttpClient httpClient = HttpClient.newBuilder()
                .sslContext(InsecureSsl.sslContext())
                .build();
        return builder -> builder.requestFactory(new JdkClientHttpRequestFactory(httpClient));
    }
}
