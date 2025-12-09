package com.space.munova.core.config;

import org.apache.http.client.config.RequestConfig;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.elasticsearch.RestClientBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticSearchConfig {

    @Value("${spring.elasticsearch.connection-timeout}")
    private int connectTimeoutMillis;

    @Value("${spring.elasticsearch.socket-timeout}")
    private int socketTimeoutMillis;

    @Value("${spring.elasticsearch.max-total-connections}")
    private int maxTotalConnections;



    @Bean
    public RestClientBuilderCustomizer elasticsearchRestClientCustomizer() {
        return (RestClientBuilder builder) -> {
            // HTTP 클라이언트 커넥션 풀 사이즈
            builder.setHttpClientConfigCallback(httpClientBuilder ->
                    httpClientBuilder
                            .setMaxConnTotal(maxTotalConnections)
            );

            // 요청별 타임아웃
            builder.setRequestConfigCallback((RequestConfig.Builder requestConfigBuilder) ->
                    requestConfigBuilder
                            .setConnectTimeout(connectTimeoutMillis)
                            .setSocketTimeout(socketTimeoutMillis)
            );
        };
    }
}
