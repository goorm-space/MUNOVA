package com.space.munova.product.ui;

import com.space.munova.core.config.ResponseApi;
import com.space.munova.product.application.CartService;
import com.space.munova.product.application.dto.cart.DeleteCartItemRequestDto;
import com.space.munova.product.application.dto.cart.FindCartInfoResponseDto;
import com.space.munova.product.application.dto.cart.AddCartItemRequestDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "장바구니", description = "장바구니 관련 API")
public class CartController {

    private final CartService cartService;

    @PostMapping("/api/cart")
    public ResponseEntity<ResponseApi<Void>> addCartItem(@RequestBody @Valid AddCartItemRequestDto reqDto) {

        cartService.addCartItem(reqDto);

        return  ResponseEntity.ok().body(ResponseApi.ok());
    }

    @DeleteMapping("/api/cart")
    public ResponseEntity<ResponseApi<Void>> deleteCartItem(@RequestParam("cartId") DeleteCartItemRequestDto reqDto) {

        cartService.deleteByCartIds(reqDto.cartIds());
        return  ResponseEntity.ok().body(ResponseApi.ok());
    }

    @GetMapping("/api/cart")
    public ResponseEntity<ResponseApi<List<FindCartInfoResponseDto>>> getCarts() {

        cartService.findCartItemByMember();
        return ResponseEntity.ok().body(ResponseApi.ok());
    }


}
