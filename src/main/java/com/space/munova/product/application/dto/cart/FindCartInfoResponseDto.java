package com.space.munova.product.application.dto.cart;

import java.util.List;
import java.util.stream.Collectors;

/**
* @param - basicInfoDto -> 사용자가 담은 장바구니 상품의 기본정보 ( 상품디테일아이디, 상품아이디, 상품명, 브랜드 . 등등)
 * @paramm - cartItemOptionInfoDtos -> 사용자가 담은 상품의 옵션 정보 Dto 리스트 - 옵션별로 여러개가 들어감.
* */
public record FindCartInfoResponseDto (CartItemBasicInfoDto basicInfoDto,
                                       List<CartItemOptionInfoDto> cartItemOptionInfoDtos){


    /// 기존에 가져온 상품정보는 옵션을 포함한 정보들이다.
    /// 따라서 상품의 기본정보(상품아이디, 디테일아이디, 이미지등등)은 CartItemBasicInfoDto로 변환한다.
    /// 옵션은 여러가지가 올수 있다. 사이즈, 컬러 등등
    /// 따라서 리스트로 변환하여 응답데이터로 변환한다.
    public static FindCartInfoResponseDto from(List<ProductInfoForCartDto> productGroup){

        ProductInfoForCartDto first = productGroup.get(0);

        // 기본 정보 생성
        CartItemBasicInfoDto basicInfo = new CartItemBasicInfoDto(
                first.productId(),
                first.cartId(),
                first.detailId(),
                first.productName(),
                first.productPrice(),
                first.productQuantity(),
                first.cartItemQuantity(),
                first.mainImgSrc(),
                first.brandName()
        );

        // 옵션 정보 생성
        List<CartItemOptionInfoDto> options = productGroup.stream()
                .filter(p -> p.optionId() != null)
                .map(p -> new CartItemOptionInfoDto(
                        p.optionId(),
                        p.optionType().name(),
                        p.optionName()
                ))
                .toList();

        return new FindCartInfoResponseDto(basicInfo, options);
    }
}
