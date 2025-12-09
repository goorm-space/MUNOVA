package com.space.munova.payment.controller;

import com.space.munova.payment.dto.ConfirmPaymentRequest;
import com.space.munova.payment.service.PaymentService;
import com.space.munova.security.jwt.JwtHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
