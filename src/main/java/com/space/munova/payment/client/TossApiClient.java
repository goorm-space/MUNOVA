package com.space.munova.payment.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.space.munova.payment.dto.CancelPaymentRequest;
import com.space.munova.payment.dto.ConfirmPaymentRequest;
import com.space.munova.payment.dto.TossPaymentResponse;
import com.space.munova.payment.exception.PaymentException;
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

    @Value("${toss-payments.encoded-secret-key}")
    private String secretKey;

    private static final String BASE_URL = "https://api.tosspayments.com/v1/payments";

    public String sendConfirmRequest(ConfirmPaymentRequest requestBody) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(String.format("%s/confirm", BASE_URL)))
                    .header("Authorization", String.format("Basic %s", secretKey))
                    .header("Content-Type", "application/json")
                    .method("POST", HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return response.body();
            } else {
                throw PaymentException.tossApiCallFailedException();
            }
        } catch (IOException e) {
            throw new RuntimeException("토스 API 통신 중 I/O 오류 발생", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("토스 API 통신 중 쓰레드 중단됨", e);
        }
    }

    public String sendCancelRequest(String paymentKey, CancelPaymentRequest requestBody) {
        try {
            String fullUrl = String.format("%s/%s/cancel", BASE_URL, paymentKey);

            ObjectMapper objectMapper = new ObjectMapper();
            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(fullUrl))
                    .header("Authorization", String.format("Basic %s", secretKey))
                    .header("Content-Type", "application/json")
                    .method("POST", HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return response.body();
            } else {
                throw PaymentException.tossApiCallFailedException();
            }
        } catch (IOException e) {
            throw new RuntimeException("토스 API 통신 중 I/O 오류 발생", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("토스 API 통신 중 쓰레드 중단됨", e);
        }
    }
}
