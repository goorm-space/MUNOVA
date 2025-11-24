package com.space.munova.velvetQ.config;

import com.space.munova.velvetQ.dto.CheckQueueRequest;
import com.space.munova.velvetQ.dto.CheckQueueResponse;
import com.space.munova.velvetQ.dto.ExternalQueueResponse;
import com.space.munova.velvetQ.exception.VelvetQException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;

@Slf4j
@Component
public class ExternalQueueApi {

    public ExternalQueueApi(
            RestTemplate restTemplate,
            @Value("${restTemplate.baseUrl}") String baseUrl,
            @Value("${restTemplate.redirectUrl}") String redirectUrl
    ) {
        this.restTemplate = restTemplate;
        this.redirectUrl = redirectUrl;
        this.baseUrl = baseUrl;
    }

    private final String baseUrl;
    private final String redirectUrl;
    private final RestTemplate restTemplate;

    // 대기열 여부 체크
    public ExternalQueueResponse callCheckQueueRequired(CheckQueueRequest request) {
        ResponseEntity<CheckQueueResponse> response = restTemplate.postForEntity(
                baseUrl + "/queue/check",
                request,
                CheckQueueResponse.class
        );

        boolean isSuccess = response.getStatusCode().is2xxSuccessful();
        if (isSuccess) {
            CheckQueueResponse body = response.getBody();
            boolean required = Objects.requireNonNull(body).required();
            return ExternalQueueResponse.of(required, redirectUrl);
        } else {
            log.error("/queue/check 호출 실패 {}, {}", response.getStatusCode(), response.getBody());
            throw VelvetQException.failExternalCallException();
        }
    }
}
