package com.space.munova.recommend.service;

import com.space.munova.core.config.ResponseApi;
import com.space.munova.member.entity.Member;
import com.space.munova.member.repository.MemberRepository;
import com.space.munova.product.application.dto.FindProductResponseDto;
import com.space.munova.product.domain.Product;
import com.space.munova.product.domain.Repository.ProductRepository;
import com.space.munova.product.domain.enums.ProductCategory;
import com.space.munova.recommend.domain.ProductRecommendation;
import com.space.munova.recommend.domain.UserActionSummary;
import com.space.munova.recommend.domain.UserRecommendation;
import com.space.munova.recommend.dto.RecommendReasonResponseDto;
import com.space.munova.recommend.dto.RecommendationsProductResponseDto;
import com.space.munova.recommend.dto.RecommendationsUserResponseDto;
import com.space.munova.recommend.repository.ProductRecommendationRepository;
import com.space.munova.recommend.repository.UserActionSummaryRepository;
import com.space.munova.recommend.repository.UserRecommendationRepository;
import com.space.munova.security.jwt.JwtHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RecommendServiceImpl implements RecommendService {

    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final UserRecommendationRepository userRecommendRepository;
    private final ProductRecommendationRepository productRecommendRepository;
    private final UserActionSummaryRepository summaryRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public List<RecommendationsUserResponseDto> getRecommendationsByMemberId(Long memberId) {
        List<UserRecommendation> recommendations;
        if(memberId==null){
            recommendations = userRecommendRepository.findAll();
        }
        else{
            recommendations=userRecommendRepository.findByMemberId(memberId);
        }
        //Dto변환
        List<RecommendationsUserResponseDto> responseList = new ArrayList<>();
        for(UserRecommendation rec : recommendations){
            responseList.add(RecommendationsUserResponseDto.builder()
                    .memberId(rec.getMember().getId())
                    .productId(rec.getProduct().getId())
                    .score(rec.getScore())
                    .createdAt(rec.getCreatedAt())
                    .build());
        }
        return responseList;
    }

    @Override
    public List<RecommendationsProductResponseDto> getRecommendationsByProductId(Long productId) {
        List<ProductRecommendation> recommendations;
        if(productId==null){
            recommendations=productRecommendRepository.findAll();
        }
        else{
            recommendations=productRecommendRepository.findBySourceProductId(productId);
        }
        //Dto변환
        List<RecommendationsProductResponseDto> responseList = new ArrayList<>();
        for(ProductRecommendation rec : recommendations){
            responseList.add(RecommendationsProductResponseDto.builder()
                    .sourceProductId(rec.getSourceProduct().getId())
                    .targetProductId(rec.getTargetProduct().getId())
                    .createdAt(rec.getCreatedAt())
                    .build());
        }
        return responseList;
    }

    //비슷한 상품 4개와 추천 4개로 총 16개 추천
    @Override
    @Transactional
    public ResponseEntity<ResponseApi<List<FindProductResponseDto>>> updateUserProductRecommend(Long userId, Long productId) {
        List<UserActionSummary> summaries= summaryRepository.findByMemberId(userId);
        if (summaries.isEmpty()) {
            return ResponseEntity.ok(ResponseApi.ok(Collections.emptyList()));
        }
        // 점수 계산
        Map<Long, Double> productScores = new HashMap<>();
        for(UserActionSummary summary : summaries){
            double score=getRecommendationScore(userId,summary.getProductId());
            productScores.put(summary.getProductId(),score);
        }
        // 점수 높은 상품 정렬
        List<Long> topProductIds=productScores.entrySet().stream()
                .sorted(Map.Entry.<Long,Double>comparingByValue().reversed())
                .limit(4)
                .map(Map.Entry::getKey)
                .toList();
        // 유사 상품 조회
        List<Product> Recommendations=new ArrayList<>();
        for(Long topId : topProductIds){
            Recommendations.addAll(
                    productRepository.findTop4ByCategory_IdAndIdNotOrderByIdAsc(
                            productRepository.findById(topId).get().getCategory().getId(),
                            topId
                    )
            );
        }
        List<FindProductResponseDto> dtoList = toFindProductResponseDtoList(Recommendations);
        for (Product rec : Recommendations) {
            UserRecommendation ur = UserRecommendation.builder()
                    .member(memberRepository.getReferenceById(userId))
                    .product(rec)
                    .score(getRecommendationScore(userId, rec.getId()))
                    .build();
            userRecommendRepository.save(ur);
        }
        return ResponseEntity.ok(ResponseApi.ok(dtoList));
    }

    @Override
    @Transactional
    public ResponseEntity<ResponseApi<List<FindProductResponseDto>>> updateSimilarProductRecommend(Long productId) {
        // 1. 상품 조회
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        productRecommendRepository.deleteBySourceProduct(product);

        if (product.getCategory() == null) {
            System.out.println("Product id=" + productId + " has no category. Cannot generate recommendations.");
            return null; // 추천 로직 종료
        }

        // 2. 같은 카테고리에서 4개 유사 상품 조회 (본 상품 제외)
        Long categoryId = product.getCategory().getId();
        List<Product> Recommendations = productRepository
                .findTop4ByCategory_IdAndIdNotOrderByIdAsc(categoryId, product.getId());

        if (Recommendations == null || Recommendations.isEmpty()) {
            System.out.println("추천할 상품이 없습니다.");
            return null;
        }
        // 3. 추천 기록 저장
        for (Product rec : Recommendations) {
            ProductRecommendation pr = ProductRecommendation.builder()
                    .sourceProduct(product)
                    .targetProduct(rec)
                    .build();
            productRecommendRepository.save(pr);

            System.out.println(product.getName() + " -> " + rec.getName());
        }
        List<FindProductResponseDto> dtoList = toFindProductResponseDtoList(Recommendations);
        return ResponseEntity.ok(ResponseApi.ok(dtoList));
    }

    private List<FindProductResponseDto> toFindProductResponseDtoList(List<Product> products) {
        return products.stream()
                .map(p -> new FindProductResponseDto(
                        p.getId(),
                        null, // mainImgSrc 없으면 null
                        p.getBrand() != null ? p.getBrand().getBrandName() : null,
                        p.getName(),
                        p.getPrice(),
                        p.getLikeCount(),
                        p.getSalesCount(),
                        p.getCreatedAt()
                ))
                .toList();
    }

    @Override
    public List<RecommendReasonResponseDto> getRecommendationReason(Long userId, Long productId) {
        List<RecommendReasonResponseDto> reasons = new ArrayList<>();

        // 최근 추천 로그(기준 상품) 조회
        UserRecommendation recentLog = userRecommendRepository.findTopByMemberIdOrderByCreatedAtDesc(userId)
                .orElse(null);
        if (recentLog == null) return reasons;

        Product baseProduct = recentLog.getProduct();
        Product targetProduct = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("추천 상품이 존재하지 않습니다."));

        // 카테고리 비교
        if (baseProduct.getCategory() != null && baseProduct.getCategory().equals(targetProduct.getCategory())) {
            reasons.add(new RecommendReasonResponseDto("category", "같은 카테고리의 상품이에요."));
        }

        // 브랜드 비교
        if (baseProduct.getBrand() != null && baseProduct.getBrand().equals(targetProduct.getBrand())) {
            reasons.add(new RecommendReasonResponseDto("brand", "같은 브랜드의 상품이에요."));
        }

        // 가격 비교
        long priceDiff = Math.abs(baseProduct.getPrice() - targetProduct.getPrice());
        if (priceDiff < 10000) {
            reasons.add(new RecommendReasonResponseDto("price", "비슷한 가격대의 상품이에요."));
        } else if (priceDiff > 30000) {
            reasons.add(new RecommendReasonResponseDto("price", "조금 다른 가격대의 상품이에요."));
        }

        // 상품명 기반 유사도 (예: “스니커즈”, “부츠”, “캔버스” 등)
        if (isNameSimilar(baseProduct.getName(), targetProduct.getName())) {
            reasons.add(new RecommendReasonResponseDto("name", "이름이 비슷한 상품이에요."));
        }

//        // 설명(info) 기반 스타일 비교
//        if (isInfoSimilar(baseProduct.getInfo(), targetProduct.getInfo())) {
//            reasons.add(new RecommendReasonResponseDto("style", "스타일이나 소재가 비슷한 상품이에요."));
//        }

        return reasons;
    }

    private boolean isNameSimilar(String name1, String name2) {
        if (name1 == null || name2 == null) return false;

        name1 = name1.toLowerCase();
        name2 = name2.toLowerCase();

        // ProductCategory의 description(예: "스니커즈", "부츠", "로퍼") 사용
        for (ProductCategory category : ProductCategory.values()) {
            String keyword = category.getDescription();
            if (name1.contains(keyword) && name2.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

//    private boolean isInfoSimilar(String info1, String info2) {
//        if (info1 == null || info2 == null) return false;
//
//        List<String> styleWords = List.of("가죽", "스웨이드", "러닝", "클래식", "스트릿", "하이탑");
//        for (String word : styleWords) {
//            if (info1.contains(word) && info2.contains(word)) {
//                return true;
//            }
//        }
//        return false;
//    }





    @Override
    public double getRecommendationScore(Long memberId, Long productId) {
        // 기본 가중치 (합계 1 이하)
        double CLICK_WEIGHT = 0.1;
        double LIKE_WEIGHT = 0.15;
        double CART_WEIGHT = 0.35;
        double PURCHASE_WEIGHT = 0.5;

        String cacheKey = "user:action:" + memberId + ":" + productId;
        UserActionSummary summary = (UserActionSummary) redisTemplate.opsForValue().get(cacheKey);

        if (summary == null) {
            summary = summaryRepository.findByMemberIdAndProductId(memberId, productId)
                    .orElse(new UserActionSummary(memberId, productId, 0, false, false, false));
            redisTemplate.opsForValue().set(cacheKey, summary, 10, TimeUnit.MINUTES);
        }

        LocalDateTime now = LocalDateTime.now();

        // 행동별 최근성 가중치 계산
        double clickedScore = summary.getClickedAt() != null ?
                CLICK_WEIGHT * Math.max(0.5, 1 - 0.05 * ChronoUnit.DAYS.between(summary.getClickedAt(), now)) : 0;
        double likedScore = Boolean.TRUE.equals(summary.getLiked()) && summary.getLikedAt() != null ?
                LIKE_WEIGHT * Math.max(0.5, 1 - 0.05 * ChronoUnit.DAYS.between(summary.getLikedAt(), now)) : 0;
        double cartScore = Boolean.TRUE.equals(summary.getInCart()) && summary.getInCartAt() != null ?
                CART_WEIGHT * Math.max(0.5, 1 - 0.05 * ChronoUnit.DAYS.between(summary.getInCartAt(), now)) : 0;
        double purchasedScore = Boolean.TRUE.equals(summary.getPurchased()) && summary.getPurchasedAt() != null ?
                PURCHASE_WEIGHT * Math.max(0.5, 1 - 0.05 * ChronoUnit.DAYS.between(summary.getPurchasedAt(), now)) : 0;

        double behaviorScore = clickedScore + likedScore + cartScore + purchasedScore;

        // 임시: 같은 카테고리일 경우 유사도 0.7
//        double similarity = 0.7;
//        double finalScore = behaviorScore * similarity;

        // 0~100 정규화
        double maxScore = CLICK_WEIGHT + LIKE_WEIGHT + CART_WEIGHT + PURCHASE_WEIGHT;
        return (behaviorScore / maxScore) * 100;
    }

    // 유저 행동 발생 시 호출
    public void updateUserAction( Long productId, Integer clicked, Boolean liked, Boolean inCart, Boolean purchased) {
        Long memberId = JwtHelper.getMemberId();
        UserActionSummary summary = summaryRepository.findByMemberIdAndProductId(memberId, productId)
                .orElse(UserActionSummary.builder()
                        .memberId(memberId)
                        .productId(productId)
                        .clicked(0)
                        .build());

        // 행동 값 업데이트
        if (clicked>0) {
            summary.setClicked(summary.getClicked() + 1);
            summary.setClickedAt(LocalDateTime.now());
        }
        if (liked != null) {
            summary.setLiked(liked);
            summary.setLikedAt(liked ? LocalDateTime.now() : null);
        }
        if (inCart != null) {
            summary.setInCart(inCart);
            summary.setInCartAt(inCart ? LocalDateTime.now() : null);
        }
        if (purchased != null) {
            summary.setPurchased(purchased);
            summary.setPurchasedAt(purchased ? LocalDateTime.now() : null);
        }

        summary.setLastUpdated(LocalDateTime.now());
        summaryRepository.save(summary);

        // Redis 캐시에 저장 (TTL 10분)
        String cacheKey = "user:action:" + memberId + ":" + productId;
        redisTemplate.opsForValue().set(cacheKey, summary, 10, TimeUnit.MINUTES);
    }

}