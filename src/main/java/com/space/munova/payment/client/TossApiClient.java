package com.space.munova.payment.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.space.munova.payment.client.exception.TossClientException;
import com.space.munova.payment.dto.CancelPaymentRequest;
import com.space.munova.payment.dto.ConfirmPaymentRequest;
import com.space.munova.payment.dto.TossPaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
@RequiredArgsConstructor
public class TossApiClient {

    private static final String BASE_URL = "https://api.tosspayments.com/v1/payments";

    @Value("${toss-payments.encoded-secret-key}")
    private String secretKey;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public TossPaymentResponse sendConfirmRequest(ConfirmPaymentRequest requestBody) {
        String path = "/confirm";

        String responseBody = executeRequest(path, requestBody);

        return parseResponse(responseBody);
    }

    public TossPaymentResponse sendCancelRequest(String paymentKey, CancelPaymentRequest requestBody) {
        String path = String.format("/%s/cancel", paymentKey);

        String responseBody = executeRequest(path, requestBody);

        return parseResponse(responseBody);
    }

    private String executeRequest(String path, Object requestBody) {
        String fullUrl = String.format("%s%s", BASE_URL, path);

        String jsonBody;
        try {
            jsonBody = objectMapper.writeValueAsString(requestBody);
        }  catch (JsonProcessingException e) {
            throw TossClientException.toJsonException("RequestBody: %s", requestBody.toString());
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .header("Authorization", String.format("Basic %s", secretKey))
                .header("Content-Type", "application/json")
                .method("POST", HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        try{
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw TossClientException.apiCallFailedException(response.toString());
            }
            return response.body();

        } catch (IOException e) {
            throw TossClientException.networkIoException();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw TossClientException.threadInterruptedError();
        }
    }

    private TossPaymentResponse parseResponse(String responseBody) {
        try {
            return objectMapper.readValue(responseBody, TossPaymentResponse.class);
        } catch (JsonProcessingException e) {
            throw TossClientException.toJsonException("ResponseBody: %s", responseBody);
        }
    }
}
