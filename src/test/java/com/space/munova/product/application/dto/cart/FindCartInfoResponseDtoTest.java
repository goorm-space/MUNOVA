package com.space.munova.product.application.dto.cart;

import com.space.munova.product.domain.enums.OptionCategory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FindCartInfoResponseDto 단위 테스트")
class FindCartInfoResponseDtoTest {

    private ProductInfoForCartDto createProductInfo(Long productId, Long cartId, Long detailId,
                                                     String productName, Long productPrice,
                                                     int productQuantity, int cartItemQuantity,
                                                     String mainImgSrc, String brandName,
                                                     Long optionId, OptionCategory optionType, String optionName) {
        return new ProductInfoForCartDto(
                productId, cartId, detailId, productName, productPrice,
                productQuantity, cartItemQuantity, mainImgSrc, brandName,
                optionId, optionType, optionName
        );
    }

    @Test
    @DisplayName("from 메서드 - 옵션이 있는 경우 성공")
    void from_WithOptions_Success() {
        // Given
        List<ProductInfoForCartDto> productGroup = new ArrayList<>();
        // 기본 정보 (옵션 없음)
        productGroup.add(createProductInfo(
                1L, 1L, 1L, "테스트 상품", 10000L,
                10, 2, "image.jpg", "브랜드1",
                null, null, null
        ));
        // 사이즈 옵션
        productGroup.add(createProductInfo(
                1L, 1L, 1L, "테스트 상품", 10000L,
                10, 2, "image.jpg", "브랜드1",
                1L, OptionCategory.SIZE, "270"
        ));
        // 컬러 옵션
        productGroup.add(createProductInfo(
                1L, 1L, 1L, "테스트 상품", 10000L,
                10, 2, "image.jpg", "브랜드1",
                2L, OptionCategory.COLOR, "빨강"
        ));

        // When
        FindCartInfoResponseDto result = FindCartInfoResponseDto.from(productGroup);

        // Then
        assertNotNull(result);
        assertNotNull(result.basicInfoDto());
        assertEquals(1L, result.basicInfoDto().productId());
        assertEquals(1L, result.basicInfoDto().cartId());
        assertEquals(1L, result.basicInfoDto().detailId());
        assertEquals("테스트 상품", result.basicInfoDto().productName());
        assertEquals(10000L, result.basicInfoDto().productPrice());
        assertEquals(10, result.basicInfoDto().productQuantity());
        assertEquals(2, result.basicInfoDto().cartItemQuantity());
        assertEquals("image.jpg", result.basicInfoDto().mainImgSrc());
        assertEquals("브랜드1", result.basicInfoDto().brandName());

        // 옵션 검증
        assertNotNull(result.cartItemOptionInfoDtos());
        assertEquals(2, result.cartItemOptionInfoDtos().size());
        
        // 사이즈 옵션 검증
        CartItemOptionInfoDto sizeOption = result.cartItemOptionInfoDtos().stream()
                .filter(o -> o.optionId().equals(1L))
                .findFirst()
                .orElse(null);
        assertNotNull(sizeOption);
        assertEquals("SIZE", sizeOption.OptionType());
        assertEquals("270", sizeOption.OptionName());

        // 컬러 옵션 검증
        CartItemOptionInfoDto colorOption = result.cartItemOptionInfoDtos().stream()
                .filter(o -> o.optionId().equals(2L))
                .findFirst()
                .orElse(null);
        assertNotNull(colorOption);
        assertEquals("COLOR", colorOption.OptionType());
        assertEquals("빨강", colorOption.OptionName());
    }

    @Test
    @DisplayName("from 메서드 - 옵션이 없는 경우 성공")
    void from_WithoutOptions_Success() {
        // Given
        List<ProductInfoForCartDto> productGroup = new ArrayList<>();
        productGroup.add(createProductInfo(
                1L, 1L, 1L, "테스트 상품", 10000L,
                10, 2, "image.jpg", "브랜드1",
                null, null, null
        ));

        // When
        FindCartInfoResponseDto result = FindCartInfoResponseDto.from(productGroup);

        // Then
        assertNotNull(result);
        assertNotNull(result.basicInfoDto());
        assertNotNull(result.cartItemOptionInfoDtos());
        assertEquals(0, result.cartItemOptionInfoDtos().size());  // 옵션이 없으므로 빈 리스트
    }

    @Test
    @DisplayName("from 메서드 - 여러 옵션이 있는 경우 성공")
    void from_MultipleOptions_Success() {
        // Given
        List<ProductInfoForCartDto> productGroup = new ArrayList<>();
        // 기본 정보
        productGroup.add(createProductInfo(
                1L, 1L, 1L, "테스트 상품", 10000L,
                10, 2, "image.jpg", "브랜드1",
                null, null, null
        ));
        // 옵션 1
        productGroup.add(createProductInfo(
                1L, 1L, 1L, "테스트 상품", 10000L,
                10, 2, "image.jpg", "브랜드1",
                1L, OptionCategory.SIZE, "270"
        ));
        // 옵션 2
        productGroup.add(createProductInfo(
                1L, 1L, 1L, "테스트 상품", 10000L,
                10, 2, "image.jpg", "브랜드1",
                2L, OptionCategory.SIZE, "280"
        ));
        // 옵션 3
        productGroup.add(createProductInfo(
                1L, 1L, 1L, "테스트 상품", 10000L,
                10, 2, "image.jpg", "브랜드1",
                3L, OptionCategory.COLOR, "빨강"
        ));

        // When
        FindCartInfoResponseDto result = FindCartInfoResponseDto.from(productGroup);

        // Then
        assertNotNull(result);
        assertEquals(3, result.cartItemOptionInfoDtos().size());  // 옵션 3개
    }

    @Test
    @DisplayName("from 메서드 - 첫 번째 요소의 정보로 기본 정보 생성")
    void from_UsesFirstElementForBasicInfo() {
        // Given
        List<ProductInfoForCartDto> productGroup = new ArrayList<>();
        productGroup.add(createProductInfo(
                1L, 10L, 100L, "첫번째 상품", 50000L,
                20, 5, "first.jpg", "첫번째 브랜드",
                null, null, null
        ));
        productGroup.add(createProductInfo(
                2L, 20L, 200L, "두번째 상품", 60000L,
                30, 6, "second.jpg", "두번째 브랜드",
                1L, OptionCategory.SIZE, "270"
        ));

        // When
        FindCartInfoResponseDto result = FindCartInfoResponseDto.from(productGroup);

        // Then
        // 첫 번째 요소의 정보가 사용되어야 함
        assertEquals(1L, result.basicInfoDto().productId());
        assertEquals(10L, result.basicInfoDto().cartId());
        assertEquals(100L, result.basicInfoDto().detailId());
        assertEquals("첫번째 상품", result.basicInfoDto().productName());
        assertEquals(50000L, result.basicInfoDto().productPrice());
        assertEquals(20, result.basicInfoDto().productQuantity());
        assertEquals(5, result.basicInfoDto().cartItemQuantity());
        assertEquals("first.jpg", result.basicInfoDto().mainImgSrc());
        assertEquals("첫번째 브랜드", result.basicInfoDto().brandName());
    }

    @Test
    @DisplayName("from 메서드 실패 - 빈 리스트")
    void from_EmptyList() {
        // Given
        List<ProductInfoForCartDto> emptyList = new ArrayList<>();

        // When & Then
        assertThrows(IndexOutOfBoundsException.class, () -> {
            FindCartInfoResponseDto.from(emptyList);
        });
    }

    @Test
    @DisplayName("from 메서드 실패 - null 리스트")
    void from_NullList() {
        // When & Then
        assertThrows(NullPointerException.class, () -> {
            FindCartInfoResponseDto.from(null);
        });
    }

    @Test
    @DisplayName("from 메서드 실패 - optionType이 null인 경우")
    void from_NullOptionType() {
        // Given
        List<ProductInfoForCartDto> productGroup = new ArrayList<>();
        productGroup.add(createProductInfo(
                1L, 1L, 1L, "테스트 상품", 10000L,
                10, 2, "image.jpg", "브랜드1",
                null, null, null
        ));
        // optionId는 있지만 optionType이 null인 경우
        productGroup.add(createProductInfo(
                1L, 1L, 1L, "테스트 상품", 10000L,
                10, 2, "image.jpg", "브랜드1",
                1L, null, "옵션명"  // optionType이 null
        ));

        // When & Then
        assertThrows(NullPointerException.class, () -> {
            FindCartInfoResponseDto.from(productGroup);
        });
    }

    @Test
    @DisplayName("from 메서드 - optionId는 null이지만 optionType이 있는 경우 (필터링되어야 함)")
    void from_OptionIdNullButOptionTypeExists() {
        // Given
        List<ProductInfoForCartDto> productGroup = new ArrayList<>();
        // optionId가 null이면 필터링되어야 함 (optionType이 있어도)
        productGroup.add(createProductInfo(
                1L, 1L, 1L, "테스트 상품", 10000L,
                10, 2, "image.jpg", "브랜드1",
                null, OptionCategory.SIZE, "270"  // optionId가 null이므로 필터링됨
        ));

        // When
        FindCartInfoResponseDto result = FindCartInfoResponseDto.from(productGroup);

        // Then
        assertNotNull(result);
        assertEquals(0, result.cartItemOptionInfoDtos().size());  // 필터링되어 옵션이 없어야 함
    }
}

