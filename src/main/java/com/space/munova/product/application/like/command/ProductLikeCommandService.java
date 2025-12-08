package com.space.munova.product.application.like.command;

import com.space.munova.member.entity.Member;
import com.space.munova.member.exception.MemberException;
import com.space.munova.member.repository.MemberRepository;
import com.space.munova.product.application.like.command.port.ProductLikeCommandPort;
import com.space.munova.product.application.product.command.event.ProductLikeEventDto;
import com.space.munova.product.application.like.command.exception.LikeException;
import com.space.munova.product.application.product.command.exception.ProductException;
import com.space.munova.product.application.product.command.port.ProductRedisCommandPort;
import com.space.munova.product.domain.Product;
import com.space.munova.product.domain.ProductLike;
import com.space.munova.product.domain.Repository.ProductLikeRepository;
import com.space.munova.product.domain.Repository.ProductRepository;
import com.space.munova.recommend.infra.RedisStreamProducer;
import com.space.munova.recommend.service.RecommendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ProductLikeCommandService {

    private final ProductLikeCommandPort productLikeCommandPort;
    private final ProductLikeRepository productLikeRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final RecommendService recommendService;
    private final RedisStreamProducer logProducer;


    public void deleteProductLikeByProductId(Long productId, Long memberId) {

        ///  멤버의 좋아요리스트 제거후 영향받은 로우카운드 리턴받음.
        int rowCount = productLikeRepository.deleteAllByProductIdsAndMemberId(productId, memberId);
        if(rowCount == 0) {
            throw LikeException.badRequestException("취소한 상품을 찾을수 없습니다.");
        }

        /// 레디스 업데이트
        productLikeCommandPort.dislike(productId, -1);
        upsertUserAction(productId,false);
    }

    public void addLike(Long productId, Long memberId) {

        Member member = memberRepository.findById(memberId).orElseThrow(MemberException::invalidMemberException);
        Product product = productRepository
                .findByIdAndIsDeletedFalse(productId).orElseThrow(()-> ProductException.badRequestException("해당 상품 정보를 찾을 수 없습니다."));

        boolean isLiked = productLikeRepository.existsByProductIdAndMemberIdAndIsDeletedFalse(productId, memberId);

        /// 좋아요 한 상풍인데 또 좋아요 눌렀을 경우 disLike
        if(isLiked) {

            ///  사용자 좋아요 리스트 제거
            productLikeRepository.deleteAllByProductIdsAndMemberId(productId, memberId);

            Map<String, Object> logData = Map.of(
                    "event_type", "cancel_product_like",
                    "service", "product",
                    "member_id", memberId,
                    "data", Map.of(
                            "product_id", productId
                    )
            );
            logProducer.sendLogAsync(RedisStreamProducer.StreamType.PRODUCT, logData);

            productLikeCommandPort.dislike(productId, -1);

        } else {
            /// 사용자 좋아요 리스트 추가
            ProductLike productLike = ProductLike.createDefaultProductLike(product, member);
            productLikeRepository.save(productLike);

            Map<String, Object> logData = Map.of(
                    "event_type", "product_like",
                    "service", "product",
                    "member_id", memberId,
                    "data", Map.of(
                            "product_id", productId
                    )
            );
            logProducer.sendLogAsync(RedisStreamProducer.StreamType.PRODUCT, logData);

            productLikeCommandPort.like(productId, 1);
        }
    }



    /// 판매자가 상품 삭제시 좋아요리스트에서 상품제거.
    public void deleteProductLikeByProductIds(List<Long> productIds) {

        productLikeRepository.deleteAllByProductIds(productIds);
    }

    private void upsertUserAction(Long productId, Boolean liked){
        recommendService.updateUserAction(productId, 0, liked, null, null);
    }


}
