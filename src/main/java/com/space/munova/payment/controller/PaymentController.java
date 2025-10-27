package com.space.munova.payment.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.space.munova.core.config.ResponseApi;
import com.space.munova.payment.dto.CancelPaymentRequest;
import com.space.munova.payment.dto.ConfirmPaymentRequest;
import com.space.munova.payment.service.PaymentService;
import io.swagger.v3.oas.models.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/payment")
public class PaymentController {

    @Value("${toss-payments.encoded-secret-key}")
    private String secretKey;

    private static final String BASE_URL = "https://api.tosspayments.com/v1/payments";
    private static final String CANCEL_PATH = "/cancel";

    private final PaymentService paymentService;

    @PostMapping("/confirm")
    public void requestTossPayments(@RequestBody ConfirmPaymentRequest requestBody) throws IOException, InterruptedException {

        ObjectMapper objectMapper = new ObjectMapper();
        String jsonBody = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(String.format("%s/confirm", BASE_URL)))
                .header("Authorization", String.format("Basic %s", secretKey))
                .header("Content-Type", "application/json")
                .method("POST", HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.body());

        paymentService.savePaymentInfo(response.body());
    }

    @PostMapping("/{paymentKey}/cancel")
    public void cancelPayment(@PathVariable String paymentKey, @RequestBody CancelPaymentRequest requestBody) throws IOException, InterruptedException {

        ObjectMapper objectMapper = new ObjectMapper();
        String jsonBody = objectMapper.writeValueAsString(requestBody);

        String urlTemplate = BASE_URL + "/%s" + CANCEL_PATH;
        String fullUrl = String.format(urlTemplate, paymentKey);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(fullUrl))
                .header("Authorization", "Basic " + secretKey)
                .header("Content-Type", "application/json")
                .method("POST", HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.body());

        // Todo: refund 테이블 저장 및 payment 업데이트

    }
}
