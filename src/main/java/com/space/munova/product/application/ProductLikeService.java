package com.space.munova.product.application;

import com.space.munova.member.entity.Member;
import com.space.munova.member.exception.MemberException;
import com.space.munova.member.repository.MemberRepository;
import com.space.munova.product.application.dto.FindProductResponseDto;
import com.space.munova.product.application.exception.LikeException;
import com.space.munova.product.domain.Product;
import com.space.munova.product.domain.ProductLike;
import com.space.munova.product.domain.Repository.ProductLikeRepository;
import com.space.munova.recommend.service.RecommendService;
import com.space.munova.security.jwt.JwtHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductLikeService {

    private final ProductLikeRepository productLikeRepository;
    private final ProductService productService;
    private final MemberRepository memberRepository;
    private final ProductImageService productImageService;
    private final RecommendService recommendService;

    @Transactional(readOnly = false)
    public void deleteProductLikeByProductId(Long productId) {
        Long memberId = JwtHelper.getMemberId();

        ///  멤버의 좋아요리스트 제거후 영향받은 로우카운드 리턴받음.
        int rowCount = productLikeRepository.deleteAllByProductIdsAndMemberId(productId, memberId);
        if(rowCount == 0) {
            throw LikeException.badRequestException("취소한 상품을 찾을수 없습니다.");
        }

        /// 상품 좋아요숫자 --
       rowCount = productService.minusLikeCountInProductIds(productId);
       if(rowCount == 0) {
           throw LikeException.badRequestException("취소한 상품을 찾을 수 없습니다.");
       }
       upsertUserAction(productId,false);
    }

    @Transactional(readOnly = false)
    public void addLike(Long productId) {

        Long memberId = JwtHelper.getMemberId();

        Member member = memberRepository.findById(memberId).orElseThrow(MemberException::invalidMemberException);
        Product product = productService.findByIdAndIsDeletedFalse(productId);

        /// 사용자 좋아요 리스트 추가
        ProductLike productLike = ProductLike.createDefaultProductLike(product, member);
        productLikeRepository.save(productLike);

        /// 상품 좋아요수 증가.
        product.plusLike();
        upsertUserAction(productId,true);
    }

    public List<FindProductResponseDto> findLikeProducts(Pageable pageable) {
        Long memberId = JwtHelper.getMemberId();

        List<FindProductResponseDto> likeProductList = productLikeRepository.findLikeProductByMemberId(pageable, memberId);
        return likeProductList.stream()
                .map(dto -> new FindProductResponseDto(
                        dto.productId(),
                        productImageService.getImgPath(dto.mainImgSrc()),
                        dto.brandName(),
                        dto.productName(),
                        dto.price(),
                        dto.likeCount(),
                        dto.salesCount(),
                        dto.createAt()
                )).toList();
    }

    private void upsertUserAction(Long productId, Boolean liked){
        recommendService.updateUserAction(productId, 0, liked, null, null);
    }
}
