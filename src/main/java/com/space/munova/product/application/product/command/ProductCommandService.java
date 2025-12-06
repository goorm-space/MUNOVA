package com.space.munova.product.application.product.command;

import com.space.munova.member.entity.Member;
import com.space.munova.product.application.event.ProductDeleteEvenForLikeDto;
import com.space.munova.product.application.event.ProductDeleteEventForCartDto;
import com.space.munova.product.application.product.command.dto.AddProductRequestDto;
import com.space.munova.product.application.product.command.exception.ProductException;
import com.space.munova.product.domain.Brand;
import com.space.munova.product.domain.Category;
import com.space.munova.product.domain.Product;
import com.space.munova.product.domain.Repository.ProductRepository;
import com.space.munova.product.infra.elasticsearch.command.ProductEsCommandRepo;
import com.space.munova.product.infra.mongo.command.ProductMongoCommandRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductCommandService {

    private final ProductRepository productRepository;


    /// 좋아요 취소 (메시지리슨)
    public int minusLikeCountInProductIds(Long productId) {
        int rowCount = productRepository.minusLikeCountInProductIds(productId);
        if(rowCount == 0) {
            throw ProductException.badRequestException("좋아요 취소 실패.");
        }
        return rowCount;
    }

    /// 좋아요 (메시지리슨)
    public void plusLikeCountByProductId(Long productId) {
        productRepository.plusLikeCountByProductId(productId);
    }


    public void updateProductViewCount(Long productId) {
        productRepository.updateProductViewCount(productId);

    }

    /// 상품 등록 메서드
    public Product saveProduct(AddProductRequestDto reqDto,
                               Member seller,
                               Brand brand,
                               Category category){

            Product product = Product.createDefaultProduct(reqDto.ProductName(),
                    reqDto.info(),
                    reqDto.price(),
                    brand,
                    category,
                    seller);

           return productRepository.save(product);
    }

    ///  현재 프로덕트를 삭제할때 카트와 좋아요를 한트랜잭션에 묶고 있지만 이후에 트랜잭션을 분리해야함.
    ///  상품 , 좋아요, 장바구니는 각각 어그리거트 루트가 다르다.
    public void deleteProduct(List<Long> ids) {

        productRepository.deleteAllByProductIds(ids);
    }


    ///  제거할 아이디 조회
    public List<Long> findDeleteProductIds(List<Long> productIds, Long sellerId) {

        List<Product> productBySeller = productRepository.findAllByIdAndMemberId(productIds, sellerId);
        List<Long> ids = productBySeller.stream().map(Product::getId).toList();

        return ids;
    }

    public Product findProductBySeller(Long productId, Long sellerId) {

        return productRepository.findProductForUpdate(productId, sellerId)
                .orElseThrow(() -> ProductException.badRequestException("등록한 상품을 찾을 수 없습니다."));
    }
}


