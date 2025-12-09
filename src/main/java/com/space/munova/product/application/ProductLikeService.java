package com.space.munova.product.application;

import com.space.munova.core.dto.PagingResponse;
import com.space.munova.log.infra.UserActionKafkaProducer;
import com.space.munova.member.repository.MemberRepository;
import com.space.munova.product.application.dto.FindProductResponseDto;
import com.space.munova.product.application.event.ProductLikeEventDto;
import com.space.munova.product.application.exception.LikeException;
import com.space.munova.product.domain.Repository.ProductLikeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductLikeService {

    private final ProductLikeRepository productLikeRepository;
    private final ProductService productService;
    private final MemberRepository memberRepository;
    private final ProductImageService productImageService;
    private final ApplicationEventPublisher eventPublisher;
    private final UserActionKafkaProducer kafkaProducer;

    @Transactional(readOnly = false)
    public void deleteProductLikeByProductId(Long productId, Long memberId) {

        ///  멤버의 좋아요리스트 제거후 영향받은 로우카운드 리턴받음.
        int rowCount = productLikeRepository.deleteAllByProductIdsAndMemberId(productId, memberId);
        if (rowCount == 0) {
            throw LikeException.badRequestException("취소한 상품을 찾을수 없습니다.");
        }

        ///  삭제메시지 발행
        ProductLikeEventDto eventDto = new ProductLikeEventDto(productId, true);
        eventPublisher.publishEvent(eventDto);
    }

    @Transactional(readOnly = false)
    public void addLike(Long productId, Long memberId) {
        // 부하 테스트용: DB 쿼리 및 비즈니스 로직 주석처리
        // Member member = memberRepository.findById(memberId).orElseThrow(MemberException::invalidMemberException);
        // Product product = productService.findByIdAndIsDeletedFalse(productId);
        // boolean isLiked = productLikeRepository.existsByProductIdAndMemberIdAndIsDeletedFalse(productId, memberId);

        // 부하 테스트용: 항상 좋아요 추가로 처리 (Kafka 메시지 전송만)
        // /// 좋아요 한 상풍인데 또 좋아요 눌렀을 경우 disLike
        // if (isLiked) {
        //     ///  사용자 좋아요 리스트 제거
        //     productLikeRepository.deleteAllByProductIdsAndMemberId(productId, memberId);
        //
        //     Map<String, Object> logData = Map.of(
        //             "event_type", "cancel_product_like",
        //             "service", "product",
        //             "member_id", memberId,
        //             "data", Map.of(
        //                     "product_id", productId
        //             )
        //     );
        //     kafkaProducer.sendLog(logData);
        //
        //     /// 좋아요 취소 메시지 발행
        //     ProductLikeEventDto eventDto = new ProductLikeEventDto(productId, true);
        //     eventPublisher.publishEvent(eventDto);
        //
        // } else {
        //     /// 사용자 좋아요 리스트 추가
        //     ProductLike productLike = ProductLike.createDefaultProductLike(product, member);
        //     productLikeRepository.save(productLike);

        // Kafka 메시지 전송만 수행
        Map<String, Object> logData = Map.of(
                "event_type", "product_like",
                "service", "product",
                "member_id", memberId,
                "data", Map.of(
                        "product_id", productId
                )
        );
        kafkaProducer.sendLog(logData);

        // ///  좋아요 메시지 발행
        // ProductLikeEventDto eventDto = new ProductLikeEventDto(productId, false);
        // eventPublisher.publishEvent(eventDto);
        // }
    }

    public PagingResponse<FindProductResponseDto> findLikeProducts(Pageable pageable, Long memberId) {

        Page<FindProductResponseDto> likeProductList = productLikeRepository.findLikeProductByMemberId(pageable, memberId);
        return PagingResponse.from(likeProductList);
    }

    @Transactional(readOnly = false)
    public void deleteProductLikeByProductIds(List<Long> productIds) {

        productLikeRepository.deleteAllByProductIds(productIds);
    }
}
