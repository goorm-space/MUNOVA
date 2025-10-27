package com.space.munova.product.application.dto.cart;

import java.util.List;

/**
* @param - basicInfoDto -> 사용자가 담은 장바구니 상품의 기본정보 ( 상품디테일아이디, 상품아이디, 상품명, 브랜드 . 등등)
 * @paramm - cartItemOptionInfoDtos -> 사용자가 담은 상품의 옵션 정보 Dto 리스트 - 옵션별로 여러개가 들어감.
* */
public record FindCartInfoResponseDto (CartItemBasicInfoDto basicInfoDto,
                                       List<CartItemOptionInfoDto> cartItemOptionInfoDtos){
}
