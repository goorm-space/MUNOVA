package com.space.munova.velvetQ.config;

import com.space.munova.core.config.ResponseApi;
import com.space.munova.velvetQ.dto.CheckQueueRequest;
import com.space.munova.velvetQ.dto.CheckQueueResponse;
import com.space.munova.velvetQ.dto.ExternalQueueResponse;
import com.space.munova.velvetQ.exception.VelvetQException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class ExternalQueueApi {

    public ExternalQueueApi(
            RestClient restClient,
            @Value("${restTemplate.requestUrl}") String requestUrl,
            @Value("${restTemplate.redirectUrl}") String redirectUrl
    ) {
        this.restClient = restClient;
        this.requestUrl = requestUrl;
        this.redirectUrl = redirectUrl;
    }

    private final RestClient restClient;
    private final String requestUrl;
    private final String redirectUrl;

    // 대기열 여부 체크
    public ExternalQueueResponse callCheckQueueRequired(CheckQueueRequest request) {
        ParameterizedTypeReference<ResponseApi<CheckQueueResponse>> typeReference =
                new ParameterizedTypeReference<>() {
                };

        ResponseEntity<ResponseApi<CheckQueueResponse>> responseEntity = restClient.post()
                .uri(requestUrl)
                .body(request)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::is5xxServerError, (cbRequest, cbResponse) -> {
                    log.error("{} 호출 실패 {}, {}", requestUrl, cbResponse.getStatusCode(), cbResponse.getBody());
                    throw VelvetQException.failExternalCallException();
                })
                .toEntity(typeReference);

        ResponseApi<CheckQueueResponse> response = responseEntity.getBody();
        if (response != null && response.getData() != null) {
            CheckQueueResponse checkResponse = response.getData();
            return ExternalQueueResponse.of(checkResponse.requiredQueue(), redirectUrl);
        }

        throw VelvetQException.failExternalCallException();
    }
}
