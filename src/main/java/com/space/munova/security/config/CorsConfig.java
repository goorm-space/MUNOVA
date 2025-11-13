package com.space.munova.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();

        // 허용할 Origin 설정
        corsConfiguration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "https://femunova.jeondoh.synology.me/",
                "https://fe-munova.vercel.app"
        ));

        // 허용할 HTTP 메서드 설정
        corsConfiguration.setAllowedMethods(Arrays.asList(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "HEAD",
                "OPTIONS"
        ));

        // 허용할 헤더 설정
        corsConfiguration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Accept",
                "X-Requested-With",
                "Cache-Control",
                "X-Device-Id"
        ));

        // 응답에 포함될 헤더 설정
        corsConfiguration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Total-Count",
                "X-Page-Count"
        ));

        corsConfiguration.setAllowCredentials(true);
        corsConfiguration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);

        return source;
    }
}
