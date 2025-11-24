package com.space.munova.payment.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.space.munova.core.config.ResponseApi;
import com.space.munova.order.exception.OrderException;
import com.space.munova.payment.client.TossApiClient;
import com.space.munova.payment.dto.CancelPaymentRequest;
import com.space.munova.payment.dto.ConfirmPaymentRequest;
import com.space.munova.payment.exception.PaymentException;
import com.space.munova.payment.service.PaymentService;
import com.space.munova.security.jwt.JwtHelper;
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
@RequestMapping("api/payment")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/confirm")
    public void requestTossPayments(@RequestBody ConfirmPaymentRequest requestBody) {
        Long memberId = JwtHelper.getMemberId();
        paymentService.confirmPaymentAndSavePayment(requestBody, memberId);
    }
}
