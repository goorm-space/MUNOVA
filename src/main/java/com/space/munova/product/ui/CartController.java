package com.space.munova.product.ui;

import com.space.munova.core.config.ResponseApi;
import com.space.munova.product.application.CartService;
import com.space.munova.product.application.dto.addCartItemRequestDto;
import com.space.munova.product.domain.Cart;
import io.swagger.v3.oas.annotations.tags.Tag;
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
    public ResponseEntity<ResponseApi<Void>> addCartItem(@RequestBody addCartItemRequestDto reqDto) {

        cartService.addCartItem(reqDto);

        return  ResponseEntity.ok().body(ResponseApi.ok());
    }

    @DeleteMapping("/api/cart")
    public ResponseEntity<ResponseApi<Void>> deleteCartItem(@RequestParam("cartId") List<Long> cartIds) {

        cartService.deleteByCartIds(cartIds);
        return  ResponseEntity.ok().body(ResponseApi.ok());
    }


}
