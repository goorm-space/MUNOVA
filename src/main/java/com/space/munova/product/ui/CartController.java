package com.space.munova.product.ui;

import com.space.munova.core.config.ResponseApi;
import com.space.munova.core.dto.PagingResponse;
import com.space.munova.product.application.CartService;
import com.space.munova.product.application.dto.cart.FindCartInfoResponseDto;
import com.space.munova.product.application.dto.cart.AddCartItemRequestDto;
import com.space.munova.product.application.dto.cart.UpdateCartRequestDto;
import com.space.munova.security.jwt.JwtHelper;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

        Long memberId = JwtHelper.getMemberId();
        cartService.addCartItem(reqDto, memberId);
        return  ResponseEntity.ok().body(ResponseApi.ok());
    }

    @DeleteMapping("/api/cart")
    public ResponseEntity<ResponseApi<Void>> deleteCartItem(@RequestParam("cartIds")  List<Long> cartIds) {
        Long memberId = JwtHelper.getMemberId();
        cartService.deleteByCartIds(cartIds, memberId);
        return  ResponseEntity.ok().body(ResponseApi.ok());
    }

    @GetMapping("/api/cart")
    public ResponseEntity<ResponseApi<PagingResponse<FindCartInfoResponseDto>>> findCartItem(@PageableDefault Pageable pageable) {

        Long memberId = JwtHelper.getMemberId();
        PagingResponse<FindCartInfoResponseDto> cartItemByMember = cartService.findCartItemByMember(pageable, memberId);
        return ResponseEntity.ok().body(ResponseApi.ok(cartItemByMember));
    }

    @PatchMapping("/api/cart")
    public ResponseEntity<ResponseApi<Void>> updateCartItem(@Valid @RequestBody UpdateCartRequestDto reqDto) {
        Long memberId = JwtHelper.getMemberId();
        cartService.updateCartByMemeber(reqDto, memberId);
        return  ResponseEntity.ok().body(ResponseApi.ok());
    }

}
