package com.space.munova.recommend.service;

import com.space.munova.member.entity.Member;
import com.space.munova.member.repository.MemberRepository;
import com.space.munova.product.domain.Product;
import com.space.munova.product.domain.Repository.ProductRepository;
import com.space.munova.product.domain.enums.ProductCategory;
import com.space.munova.recommend.domain.ProductRecommendation;
import com.space.munova.recommend.domain.UserActionSummary;
import com.space.munova.recommend.domain.UserRecommendation;
import com.space.munova.recommend.dto.RecommendReasonResponseDto;
import com.space.munova.recommend.dto.ResponseDto;
import com.space.munova.recommend.repository.ProductRecommendationRepository;
import com.space.munova.recommend.repository.UserActionSummaryRepository;
import com.space.munova.recommend.repository.UserRecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    public List<ResponseDto> getRecommendationsByUserId(Long userId) {
        //회원 추천 로그 조회 로직
        return Collections.emptyList();
    }

    @Override
    public void createUserRecommendLog(Long userId) {
        //회원 추천 로그 로직
        System.out.println("User recommend log created for userId = " + userId);
    }

    @Override
    public void updateUserRecommendLog(Long userId) {
        //회원 추천 로그 로직
        System.out.println("User recommend log updated for userId = " + userId);
    }

    @Override
    public List<ResponseDto> getRecommendationsByProductId(Long productId) {
        //상품 추천 로그 조회 로직
        return Collections.emptyList();
    }

    @Override
    public void createProductRecommendLog() {
        //상품 추천 로그 로직
        System.out.println("Product recommend log created");
    }

    @Override
    public void updateProductRecommendLog(Long productId) {
        //상품 추천 로그 로직
        System.out.println("Product recommend log updated for productId = " + productId);
    }

    @Override
    @Transactional
    public void updateUserProductRecommend(Long userId, Long productId) {
        // 1. 사용자 조회
        Member user = memberRepository.findById(userId)
                .orElseThrow();

        // 2. 클릭/좋아요/장바구니 기준 상품 조회
        Product clickedProduct = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // 3. 같은 카테고리에서 4개 추천 상품 조회
        List<Product> recommendedProducts = productRepository
                .findTop4ByCategory_IdAndIdNotOrderByIdAsc(clickedProduct.getCategory().getId(), clickedProduct.getId());
        double recommendedScore=getRecommendationScore(userId,productId);
        // 4. 추천 기록 저장
        for (Product rec : recommendedProducts) {
            UserRecommendation ur = UserRecommendation.builder()
                    .member(user)
                    .product(rec)
                    .score(recommendedScore)
                    .build();
            userRecommendRepository.save(ur);
        }

        System.out.println("User recommendations updated for user " + userId + " based on product " + productId);
    }

    @Override
    @Transactional
    public void updateSimilarProductRecommend(Long productId) {
        // 1. 상품 조회
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        productRecommendRepository.deleteBySourceProduct(product);

        if (product.getCategory() == null) {
            System.out.println("Product id=" + productId + " has no category. Cannot generate recommendations.");
            return; // 추천 로직 종료
        }

        // 2. 같은 카테고리에서 4개 유사 상품 조회 (본 상품 제외)
        Long categoryId = product.getCategory().getId();
        List<Product> Recommendations = productRepository
                .findTop4ByCategory_IdAndIdNotOrderByIdAsc(categoryId, product.getId());

        if (Recommendations == null || Recommendations.isEmpty()) {
            System.out.println("추천할 상품이 없습니다.");
            return;
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

        System.out.println("Similar products updated for productId = " + productId);
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
        double CLICK_WEIGHT = 0.05;
        double LIKE_WEIGHT = 0.15;
        double CART_WEIGHT = 0.3;
        double PURCHASE_WEIGHT = 0.5;

        String cacheKey = "user:action:" + memberId + ":" + productId;
        UserActionSummary summary = (UserActionSummary) redisTemplate.opsForValue().get(cacheKey);

        if (summary == null) {
            summary = summaryRepository.findByMemberIdAndProductId(memberId, productId)
                    .orElse(new UserActionSummary(memberId, productId, false, false, false, false));
            redisTemplate.opsForValue().set(cacheKey, summary, 10, TimeUnit.MINUTES);
        }

        LocalDateTime now = LocalDateTime.now();

        // 행동별 최근성 가중치 계산
        double clickedScore = summary.isClicked() && summary.getClickedAt() != null ?
                CLICK_WEIGHT * Math.max(0.5, 1 - 0.05 * ChronoUnit.DAYS.between(summary.getClickedAt(), now)) : 0;
        double likedScore = summary.isLiked() && summary.getLikedAt() != null ?
                LIKE_WEIGHT * Math.max(0.5, 1 - 0.05 * ChronoUnit.DAYS.between(summary.getLikedAt(), now)) : 0;
        double cartScore = summary.isInCart() && summary.getIncartAt() != null ?
                CART_WEIGHT * Math.max(0.5, 1 - 0.05 * ChronoUnit.DAYS.between(summary.getIncartAt(), now)) : 0;
        double purchasedScore = summary.isPurchased() && summary.getPurchasedAt() != null ?
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
    public void updateUserAction(Long memberId, Long productId, boolean clicked, boolean liked,
                                 boolean inCart, boolean purchased) {
        UserActionSummary summary = summaryRepository.findByMemberIdAndProductId(memberId, productId)
                .orElse(UserActionSummary.builder()
                        .memberId(memberId)
                        .productId(productId)
                        .build());

        // 행동 값 업데이트
        if (clicked) { summary.setClicked(true); summary.setClickedAt(LocalDateTime.now()); }
        if (liked) { summary.setLiked(true); summary.setLikedAt(LocalDateTime.now()); }
        if (inCart) { summary.setInCart(true); summary.setIncartAt(LocalDateTime.now()); }
        if (purchased) { summary.setPurchased(true); summary.setPurchasedAt(LocalDateTime.now()); }

        summary.setLastUpdated(LocalDateTime.now());
        summaryRepository.save(summary);

        // Redis 캐시에 저장 (TTL 10분)
        String cacheKey = "user:action:" + memberId + ":" + productId;
        redisTemplate.opsForValue().set(cacheKey, summary, 10, TimeUnit.MINUTES);
    }

}