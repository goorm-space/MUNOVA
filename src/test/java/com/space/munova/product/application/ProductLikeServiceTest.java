package com.space.munova.product.application;

import com.space.munova.core.dto.PagingResponse;
import com.space.munova.member.dto.MemberRole;
import com.space.munova.member.entity.Member;
import com.space.munova.member.repository.MemberRepository;
import com.space.munova.product.application.dto.FindProductResponseDto;
import com.space.munova.product.domain.*;
import com.space.munova.product.domain.Repository.ProductLikeRepository;
import com.space.munova.product.domain.enums.ProductCategory;
import com.space.munova.recommend.service.RecommendService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@DisplayName("ProductLikeService 단위 테스트")
class ProductLikeServiceTest {

    @InjectMocks
    ProductLikeService productLikeService;
    @Mock
    ProductLikeRepository productLikeRepository;
    @Mock
    ProductService productService;
    @Mock
    MemberRepository memberRepository;
    @Mock
    ProductImageService productImageService;
    @Mock
    RecommendService recommendService;
    @Mock
    ApplicationEventPublisher eventPublisher;


    private Brand createBrand() {
        return new Brand(1L, "대표자명", "BRAND001", "테스트 브랜드");
    }

    private Category createCategory() {
        return new Category(1L, null, ProductCategory.M_SNEAKERS, "테스트 카테고리", 1);
    }

    private Product createProduct(int id) {
        return Product.builder()
                .id((long) id)
                .category(createCategory())
                .brand(createBrand())
                .name("test")
                .price(12312L)
                .member(createSeller())
                .info("dksdfjsldkjflskdjfk")
                .build();
    }

    private ProductDetail createProductDetail(Long detailId, Integer quantity) {


        return ProductDetail.builder()
                .id(detailId)
                .product(createProduct(1))
                .quantity(quantity)
                .isDeleted(false)
                .build();
    }

    private Member createSeller() {
        return Member.builder()
                .id(1L)
                .role(MemberRole.SELLER)
                .username("test")
                .build();
    }

    private Member createMember() {
        return Member.builder()
                .id(2L)
                .role(MemberRole.USER)
                .username("test")
                .build();
    }

    private List<ProductLike> createProductLikes() {
        List<ProductLike> productLikes = new ArrayList<>();
        for(int i = 1; i < 11; i++) {
            ProductLike pl = ProductLike.builder()
                    .id((long) i)
                    .member(createMember())
                    .product(createProduct(i))
                    .build();
            productLikes.add(pl);
        }
        return productLikes;
    }

    ///  통합테스트
//    @DisplayName("좋아요 제거")
//    @Test
//    void 좋아요리스트에서_좋아요제거() {
//
//        ///given
//        Long memberId = 2L;
//        List<ProductLike> productLikes = createProductLikes();
//
//        ///when
//
//        ///then
//
//    }

    /// 통합테스트
//    @Test
//    void deleteProductLikeByProductId() {
//    }


    ///  통합테스트
//    @Test
//    void addLike() {
//    }


    @DisplayName("좋아요 상품 조회 - 성공")
    @Test
    void findLikeProducts_Success() {
        //given
        Member member = createMember();
        Pageable pageable = PageRequest.of(0, 10);

        List<FindProductResponseDto> content = new ArrayList<>();
        content.add(new FindProductResponseDto(1L, "img1.jpg", "브랜드1", "상품1", 10000L, 5, 10, LocalDateTime.now()));
        content.add(new FindProductResponseDto(2L, "img2.jpg", "브랜드2", "상품2", 20000L, 3, 5, LocalDateTime.now()));

        Page<FindProductResponseDto> page = new PageImpl<>(content, pageable, content.size());

        when(productLikeRepository.findLikeProductByMemberId(pageable, member.getId()))
                .thenReturn(page);

        //when
        PagingResponse<FindProductResponseDto> result =
                productLikeService.findLikeProducts(pageable, member.getId());

        //then
        assertEquals(2, result.content().size());
        assertEquals(1L, result.content().get(0).productId());
        assertEquals(2L, result.content().get(1).productId());
        assertEquals(0, result.page());
        assertEquals(10, result.size());
        assertEquals(2L, result.totalElements());
        assertEquals(1, result.totalPages());
        assertTrue(result.first());
        assertTrue(result.last());
        assertEquals(content, result.content());
    }

    @DisplayName("좋아요 상품 조회 - 빈 결과")
    @Test
    void findLikeProducts_EmptyResult() {
        //given
        Member member = createMember();
        Pageable pageable = PageRequest.of(0, 10);

        Page<FindProductResponseDto> emptyPage = new PageImpl<>(new ArrayList<>(), pageable, 0L);

        when(productLikeRepository.findLikeProductByMemberId(pageable, member.getId()))
                .thenReturn(emptyPage);

        //when
        PagingResponse<FindProductResponseDto> result =
                productLikeService.findLikeProducts(pageable, member.getId());

        //then
        assertTrue(result.content().isEmpty());
        assertEquals(0L, result.totalElements());
        assertEquals(0, result.totalPages());
        assertTrue(result.first());
        assertTrue(result.last());
    }

    @DisplayName("좋아요 상품 조회 - 여러 페이지 중 첫 페이지")
    @Test
    void findLikeProducts_FirstPage() {
        //given
        Member member = createMember();
        Pageable pageable = PageRequest.of(0, 10);
        long totalElements = 25L; // 총 25개, 페이지당 10개 = 3페이지

        List<FindProductResponseDto> content = new ArrayList<>();
        for (long i = 1; i <= 10; i++) {
            content.add(new FindProductResponseDto(i, "img" + i + ".jpg", "브랜드" + i, "상품" + i, 10000L * i, 5, 10, LocalDateTime.now()));
        }

        Page<FindProductResponseDto> page = new PageImpl<>(content, pageable, totalElements);

        when(productLikeRepository.findLikeProductByMemberId(pageable, member.getId()))
                .thenReturn(page);

        //when
        PagingResponse<FindProductResponseDto> result =
                productLikeService.findLikeProducts(pageable, member.getId());

        //then
        assertEquals(10, result.content().size());
        assertEquals(0, result.page());
        assertEquals(10, result.size());
        assertEquals(25L, result.totalElements());
        assertEquals(3, result.totalPages());
        assertTrue(result.first());
        assertFalse(result.last());
    }

    @DisplayName("좋아요 상품 조회 - 여러 페이지 중 마지막 페이지")
    @Test
    void findLikeProducts_LastPage() {
        //given
        Member member = createMember();
        Pageable pageable = PageRequest.of(2, 10); // 3번째 페이지 (0-based)
        long totalElements = 25L; // 총 25개, 페이지당 10개 = 3페이지

        List<FindProductResponseDto> content = new ArrayList<>();
        for (long i = 21; i <= 25; i++) {
            content.add(new FindProductResponseDto(i, "img" + i + ".jpg", "브랜드" + i, "상품" + i, 10000L * i, 5, 10, LocalDateTime.now()));
        }

        Page<FindProductResponseDto> page = new PageImpl<>(content, pageable, totalElements);

        when(productLikeRepository.findLikeProductByMemberId(pageable, member.getId()))
                .thenReturn(page);

        //when
        PagingResponse<FindProductResponseDto> result =
                productLikeService.findLikeProducts(pageable, member.getId());

        //then
        assertEquals(5, result.content().size());
        assertEquals(2, result.page());
        assertEquals(10, result.size());
        assertEquals(25L, result.totalElements());
        assertEquals(3, result.totalPages());
        assertFalse(result.first());
        assertTrue(result.last());
    }


    ///  통합테스트
//    @Test
//    void deleteProductLikeByProductIds() {
//    }
}