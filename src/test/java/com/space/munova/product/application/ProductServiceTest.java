package com.space.munova.product.application;

import com.space.munova.member.entity.Member;
import com.space.munova.member.repository.MemberRepository;
import com.space.munova.product.application.dto.*;
import com.space.munova.product.domain.Brand;
import com.space.munova.product.domain.Category;
import com.space.munova.product.domain.Product;
import com.space.munova.product.domain.Repository.ProductClickLogRepository;
import com.space.munova.product.domain.Repository.ProductRepository;
import com.space.munova.product.domain.Repository.ProductSearchLogRepository;
import com.space.munova.product.domain.enums.ProductCategory;
import com.space.munova.recommend.service.RecommendService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService 단위 테스트")
class ProductServiceTest {

    @Mock
    private ProductClickLogRepository productClickLogRepository;
    
    @Mock
    private ProductRepository productRepository;
    
    @Mock
    private ProductImageService productImageService;
    
    @Mock
    private ProductDetailService productDetailService;
    
    @Mock
    private BrandService brandService;
    
    @Mock
    private CategoryService categoryService;
    
    @Mock
    private MemberRepository memberRepository;
    
    @Mock
    private ProductOptionService productOptionService;
    
    @Mock
    private ProductSearchLogRepository productSearchLogRepository;
    
    @Mock
    private RecommendService recommendService;
    
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ProductService productService;

    private Member createMember() {
        return Member.createMember("testuser", "encodedPassword", "서울시");
    }

    private Brand createBrand() {
        return new Brand(1L, "대표자명", "BRAND001", "테스트 브랜드");
    }

    private Category createCategory() {
        return new Category(1L, null, ProductCategory.M_SNEAKERS, "테스트 카테고리", 1);
    }

    private Product createProduct() {
        Brand brand = createBrand();
        Category category = createCategory();
        Member member = createMember();
        return Product.createDefaultProduct(
                "테스트 상품",
                "이것은 테스트 상품입니다. 최소 10자 이상입니다.",
                100000L,
                brand,
                category,
                member
        );
    }

    @Test
    @DisplayName("판매자 상품 등록 성공")
    void saveProduct_Success() throws IOException {
        // Given
        Long sellerId = 1L;
        Member seller = createMember();
        Brand brand = createBrand();
        Category category = createCategory();
        Product savedProduct = createProduct();
        savedProduct = Product.builder()
                .id(1L)
                .name("테스트 상품")
                .info("이것은 테스트 상품입니다. 최소 10자 이상입니다.")
                .price(100000L)
                .brand(brand)
                .category(category)
                .member(seller)
                .build();

        MultipartFile mainImgFile = mock(MultipartFile.class);
        List<MultipartFile> sideImgFiles = new ArrayList<>();
        MultipartFile sideImgFile1 = mock(MultipartFile.class);
        sideImgFiles.add(sideImgFile1);

        List<ShoeOptionDto> shoeOptionDtos = new ArrayList<>();
        shoeOptionDtos.add(new ShoeOptionDto(1L, "빨강", 1L, "270", 10));
        shoeOptionDtos.add(new ShoeOptionDto(2L, "파랑", 2L, "280", 5));

        AddProductRequestDto reqDto = new AddProductRequestDto(
                "테스트 상품",
                100000L,
                "이것은 테스트 상품입니다. 최소 10자 이상입니다.",
                1L,
                1L,
                null,
                shoeOptionDtos
        );

        when(memberRepository.findById(sellerId)).thenReturn(Optional.of(seller));
        when(brandService.findById(1L)).thenReturn(brand);
        when(categoryService.findById(1L)).thenReturn(category);
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);
        doNothing().when(productImageService).saveMainImg(any(MultipartFile.class), any(Product.class));
        doNothing().when(productImageService).saveSideImg(anyList(), any(Product.class));
        doNothing().when(productDetailService).saveProductDetailAndOption(any(Product.class), anyList());

        // When
        productService.saveProduct(mainImgFile, sideImgFiles, reqDto, sellerId);

        // Then
        verify(memberRepository, times(1)).findById(sellerId);
        verify(brandService, times(1)).findById(1L);
        verify(categoryService, times(1)).findById(1L);
        verify(productRepository, times(1)).save(any(Product.class));
        verify(productImageService, times(1)).saveMainImg(any(MultipartFile.class), any(Product.class));
        verify(productImageService, times(1)).saveSideImg(anyList(), any(Product.class));
        verify(productDetailService, times(1)).saveProductDetailAndOption(any(Product.class), anyList());
    }

    @Test
    @DisplayName("판매자 상품 수정 성공")
    void updateProductInfo_Success() throws IOException {
        // Given
        Long sellerId = 1L;
        Long productId = 1L;
        Member seller = createMember();
        Brand brand = createBrand();
        Category category = createCategory();
        
        Product existingProduct = Product.builder()
                .id(productId)
                .name("기존 상품명")
                .info("기존 상품 정보입니다. 최소 10자 이상입니다.")
                .price(50000L)
                .brand(brand)
                .category(category)
                .member(seller)
                .isDeleted(false)
                .build();

        MultipartFile mainImgFile = mock(MultipartFile.class);
        when(mainImgFile.isEmpty()).thenReturn(false);
        
        List<MultipartFile> sideImgFiles = new ArrayList<>();
        MultipartFile sideImgFile1 = mock(MultipartFile.class);
        sideImgFiles.add(sideImgFile1);
        // sideImgFile은 List의 isEmpty()를 체크하므로 개별 파일의 isEmpty() stubbing 불필요

        List<Long> deletedImgIds = new ArrayList<>();
        deletedImgIds.add(1L);

        List<ShoeOptionDto> addShoeOptionDtos = new ArrayList<>();
        addShoeOptionDtos.add(new ShoeOptionDto(3L, "초록", 3L, "290", 15));

        List<UpdateQuantityDto> updateQuantityDtos = new ArrayList<>();
        updateQuantityDtos.add(new UpdateQuantityDto(1L, 20));

        List<Long> deleteDetailIds = new ArrayList<>();
        deleteDetailIds.add(2L);

        UpdateProductRequestDto reqDto = new UpdateProductRequestDto(
                productId,
                false,
                deletedImgIds,
                "수정된 상품명",
                150000L,
                "수정된 상품 정보입니다. 최소 10자 이상입니다.",
                new AddShoeOptionDto(addShoeOptionDtos),
                updateQuantityDtos,
                new DeleteProductDetailDto(deleteDetailIds)
        );

        when(productRepository.findByIdAndMemberIdAndIsDeletedFalse(productId, sellerId))
                .thenReturn(Optional.of(existingProduct));
        doNothing().when(productImageService).updateMainImg(any(MultipartFile.class), any(Product.class));
        doNothing().when(productImageService).saveSideImg(anyList(), any(Product.class));
        doNothing().when(productImageService).deleteImagesByImgIds(anyList(), any(Long.class));
        doNothing().when(productDetailService).saveProductDetailAndOption(any(Product.class), anyList());
        doNothing().when(productDetailService).updateQuantity(anyList());
        doNothing().when(productDetailService).deleteProductDetailByIds(anyList());

        // When
        productService.updateProductInfo(mainImgFile, sideImgFiles, reqDto, sellerId);

        // Then
        verify(productRepository, times(1)).findByIdAndMemberIdAndIsDeletedFalse(productId, sellerId);
        verify(productImageService, times(1)).updateMainImg(any(MultipartFile.class), any(Product.class));
        verify(productImageService, times(1)).saveSideImg(anyList(), any(Product.class));
        verify(productImageService, times(1)).deleteImagesByImgIds(anyList(), any(Long.class));
        verify(productDetailService, times(1)).saveProductDetailAndOption(any(Product.class), anyList());
        verify(productDetailService, times(1)).updateQuantity(anyList());
        verify(productDetailService, times(1)).deleteProductDetailByIds(anyList());
        
        // 상품 정보가 수정되었는지 확인
        assertEquals("수정된 상품명", existingProduct.getName());
        assertEquals("수정된 상품 정보입니다. 최소 10자 이상입니다.", existingProduct.getInfo());
        assertEquals(150000L, existingProduct.getPrice());
    }
}

