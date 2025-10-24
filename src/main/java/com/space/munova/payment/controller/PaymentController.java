package com.space.munova.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.space.munova.core.config.ResponseApi;
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

    @Value("${toss-payments.client-key}")
    private String clientKey;

    @Value("${toss-payments.encoded-secret-key}")
    private String secretKey;

    private final PaymentService paymentService;

    @PostMapping("/confirm")
    public void requestTossPayments(@RequestBody ConfirmPaymentRequest requestBody) throws IOException, InterruptedException {

        ObjectMapper objectMapper = new ObjectMapper();
        String jsonBody = objectMapper.writeValueAsString(requestBody);
        System.out.println("jsonBody: " + jsonBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.tosspayments.com/v1/payments/confirm"))
                .header("Authorization", "Basic " + secretKey)
                .header("Content-Type", "application/json")
                .method("POST", HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.body());

        // Todo: 결제 정보를 저장하자
        paymentService.savePaymentInfo(response.body());
    }
}
