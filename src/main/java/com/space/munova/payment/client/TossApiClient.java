package com.space.munova.payment.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.space.munova.payment.client.exception.TossClientException;
import com.space.munova.payment.dto.CancelPaymentRequest;
import com.space.munova.payment.dto.ConfirmPaymentRequest;
import com.space.munova.payment.dto.ReceiptInfo;
import com.space.munova.payment.dto.TossPaymentResponse;
import com.space.munova.payment.entity.PaymentMethod;
import com.space.munova.payment.entity.PaymentStatus;
import com.space.munova.payment.exception.PaymentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.ZonedDateTime;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class TossApiClient {

    private static final String BASE_URL = "https://api.tosspayments.com/v1/payments";

    @Value("${toss-payments.encoded-secret-key}")
    private String secretKey;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * mocking async
     */
    @Async("paymentExecutor")
    public CompletableFuture<TossPaymentResponse> asyncConfirmRequest(ConfirmPaymentRequest request) {
//        boolean paymentSuccess = Math.random() < 0.99;
//
//        try {
//            Thread.sleep(3000);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
//
//        if (!paymentSuccess) {
//            throw TossClientException.apiCallFailedException();
//        }

        TossPaymentResponse response = new TossPaymentResponse(request.paymentKey(), request.orderId(), PaymentStatus.DONE, PaymentMethod.카드, request.amount(), ZonedDateTime.now(), ZonedDateTime.now(), new ReceiptInfo(null), null, null);

        if (!response.status().isDone()) {
            throw PaymentException.paymentStatusException(
                    String.format("PaymentStatus는 'DONE' 상태여야 합니다. 현재 상태: '%s'", response.status().name())
            );
        }

        return CompletableFuture.completedFuture(response);
    }

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
        } catch (JsonProcessingException e) {
            throw TossClientException.toJsonException("RequestBody: %s", requestBody.toString());
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .header("Authorization", String.format("Basic %s", secretKey))
                .header("Content-Type", "application/json")
                .method("POST", HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        try {
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
