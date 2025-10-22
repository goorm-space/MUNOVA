package com.space.munova.product.application;

import com.space.munova.member.dto.MemberRole;
import com.space.munova.member.entity.Member;
import com.space.munova.product.application.dto.AddProductRequestDto;
import com.space.munova.product.application.dto.ProductCategoryResponseDto;
import com.space.munova.product.domain.Brand;
import com.space.munova.product.domain.Category;
import com.space.munova.product.domain.Product;
import com.space.munova.product.domain.Repository.MemberRepository;
import com.space.munova.product.domain.Repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductImageService productImageService;
    private final ProductDetailService productDetailService;
    private final BrandService brandService;
    private final CategoryService categoryService;
    private final MemberRepository memberRepository;
    /// 모든 카테고리 조회 메서드
    public List<ProductCategoryResponseDto> findProductCategories() {
        return categoryService.findAllProductCategories();
    }

    @Transactional
    public void saveProduct(MultipartFile mainImgFile, List<MultipartFile> sideImgFile, Long sellerId, Long brandId, AddProductRequestDto reqDto) throws IOException {

        ///멤버서비스에서 member객체를가져온다.
        Member seller = memberRepository.findById(sellerId).orElseThrow(() -> new IllegalArgumentException("Seller id not found."));

        // 브랜드 조회.
        Brand brand = brandService.findById(brandId);

        //카테고리 조회.
        Category category = categoryService.findById(reqDto.categoryId());

        // 상품생성
        Product product = Product.createDefaultProduct(reqDto.ProductName(),
                reqDto.info(),
                reqDto.price(),
                brand,
                category,
                seller);
        Product savedProduct = productRepository.save(product);


        // 이미지 저장.
        productImageService.saveMainImg(mainImgFile, savedProduct);
        productImageService.saveSideImg(sideImgFile, savedProduct);

        // 상품 디테일 옵션 저장.
        productDetailService.saveProductDetailAndOption(savedProduct, reqDto.shoeOptionDtos());
    }


}
