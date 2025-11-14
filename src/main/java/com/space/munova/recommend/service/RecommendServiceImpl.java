package com.space.munova.recommend.service;

import com.space.munova.core.config.ResponseApi;
import com.space.munova.core.dto.PagingResponse;
import com.space.munova.member.repository.MemberRepository;
import com.space.munova.product.application.dto.FindProductResponseDto;
import com.space.munova.product.domain.Category;
import com.space.munova.product.domain.Product;
import com.space.munova.product.domain.Repository.ProductRepository;
import com.space.munova.product.domain.enums.ProductCategory;
import com.space.munova.recommend.domain.ProductRecommendation;
import com.space.munova.recommend.domain.UserActionSummary;
import com.space.munova.recommend.domain.UserRecommendation;
import com.space.munova.recommend.dto.RecommendProductResponseDto;
import com.space.munova.recommend.dto.RecommendReasonResponseDto;
import com.space.munova.recommend.dto.RecommendationsProductResponseDto;
import com.space.munova.recommend.dto.RecommendationsUserResponseDto;
import com.space.munova.recommend.exception.RecommendException;
import com.space.munova.recommend.repository.ProductRecommendationRepository;
import com.space.munova.recommend.repository.UserActionSummaryRepository;
import com.space.munova.recommend.repository.UserRecommendationRepository;
import com.space.munova.security.jwt.JwtHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendServiceImpl implements RecommendService {

    private static final double CLICK_WEIGHT = 0.1;
    private static final double LIKE_WEIGHT = 0.15;
    private static final double CART_WEIGHT = 0.35;
    private static final double PURCHASE_WEIGHT = 0.5;
    private static final double DECAY_RATE = 0.05; // 하루당 5% 감쇠
    private static final double MIN_DECAY = 0.5;   // 최소 유지 비율
    private static final long CACHE_TTL_MINUTES = 10;

    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final UserRecommendationRepository userRecommendRepository;
    private final ProductRecommendationRepository productRecommendRepository;
    private final UserActionSummaryRepository summaryRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private final RestTemplate restTemplate = new RestTemplate();


    @Override
    public PagingResponse<RecommendationsUserResponseDto> getRecommendationsByMemberId(Long memberId, Pageable pageable) {
        Page<UserRecommendation> recommendations;
        if(memberId==null){
            recommendations = userRecommendRepository.findAll(pageable);
        }
        else{
            recommendations=userRecommendRepository.findByMemberId(memberId,pageable);
        }

        Page<RecommendationsUserResponseDto> dtoPage=recommendations.map(rec->
                RecommendationsUserResponseDto.builder()
                        .memberId(rec.getMember().getId())
                        .productId(rec.getProduct().getId())
                        .score(rec.getScore())
                        .createdAt(rec.getCreatedAt())
                        .build());
        return PagingResponse.from(dtoPage);
    }

    @Override
    public PagingResponse<RecommendationsProductResponseDto> getRecommendationsByProductId(Long productId, Pageable pageable) {
        Page<ProductRecommendation> recommendations;

        // 1️⃣ 조건에 따라 페이징 조회
        if (productId == null) {
            recommendations = productRecommendRepository.findAll(pageable);
        } else {
            recommendations = productRecommendRepository.findBySourceProductId(productId, pageable);
        }

        // 2️⃣ Page.map() 사용해서 DTO로 변환 (페이징 정보 그대로 유지됨)
        Page<RecommendationsProductResponseDto> dtoPage = recommendations.map(rec ->
                RecommendationsProductResponseDto.builder()
                        .sourceProductId(rec.getSourceProduct().getId())
                        .targetProductId(rec.getTargetProduct().getId())
                        .createdAt(rec.getCreatedAt())
                        .build()
        );

        // 3️⃣ PagingResponse로 변환 후 반환
        return PagingResponse.from(dtoPage);
    }

    //비슷한 상품 4개와 추천 4개로 총 16개 추천
    @Override
    @Transactional
    public ResponseEntity<ResponseApi<List<FindProductResponseDto>>> updateUserProductRecommend( Long productId) {
//        String url ="http://localhost:8001/api/recommend/user/"+productId;
//        ResponseEntity<String> response=restTemplate.exchange(url, HttpMethod.PUT,null,String.class);
//        ResponseApi<String> apiResponse = ResponseApi.ok(response.getBody());
//
//        return ResponseEntity.ok((ResponseApi) apiResponse);

        Long memberId = JwtHelper.getMemberId();

        userRecommendRepository.deleteByMemberId(memberId);
        List<UserActionSummary> summaries= summaryRepository.findByMemberId(memberId);
        if (summaries.isEmpty()) {
            return ResponseEntity.ok(ResponseApi.ok(Collections.emptyList()));
        }
        // 점수 계산
        List<Long> topProductIds = summaries.stream()
                .sorted(Comparator.comparingDouble(
                        s -> -getRecommendationScore(s.getMemberId(), s.getProductId())
                ))
                .limit(8)
                .map(UserActionSummary::getProductId)
                .toList();
        // 유사 상품 조회
        List<FindProductResponseDto> recommendations = topProductIds.stream()
                .map(productRepository::findProductSummaryById)
                .filter(Objects::nonNull)
                .toList();

        recommendations.forEach(r->{
            UserRecommendation ur=UserRecommendation.builder()
                    .member(memberRepository.getReferenceById(memberId))
                    .product(Product.builder().id(r.productId()).build())
                    .score(getRecommendationScore(memberId,r.productId()))
                    .build();
            userRecommendRepository.save(ur);
        });

        return ResponseEntity.ok(ResponseApi.ok(recommendations));
    }

    @Override
    @Transactional
    public ResponseEntity<ResponseApi<List<FindProductResponseDto>>> updateSimilarProductRecommend(Long productId) {
//        String url = "http://localhost:8001/recommend/" + productId;
//        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, null, String.class);
//        ResponseApi<String> apiResponse = ResponseApi.ok(response.getBody());
//
//        return ResponseEntity.ok((ResponseApi) apiResponse);
//
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> RecommendException.productNotFound("id=" + productId));

        productRecommendRepository.deleteBySourceProduct(product);

        List<FindProductResponseDto> recommendations = findSimilarProductsByCategory(productId, 4);

        if (recommendations.isEmpty()) {
            return ResponseEntity.ok(ResponseApi.ok(Collections.emptyList()));
        }

        recommendations.forEach(r->{
            ProductRecommendation pr = ProductRecommendation.builder()
                    .sourceProduct(product)
                    .targetProduct(Product.builder().id(r.productId()).build())
                    .build();
            productRecommendRepository.save(pr);
        });

        return ResponseEntity.ok(ResponseApi.ok(recommendations));
    }

    private List<FindProductResponseDto> findSimilarProductsByCategory(Long productId, int limit) {
        Product base = productRepository.findById(productId)
                .orElseThrow(() -> RecommendException.productNotFound("id=" + productId));

        Category category = base.getCategory();
        if (category == null) {
            throw RecommendException.categoryNotFound("productId=" + productId);
        }
        Long refCategoryId = (category.getRefCategory() != null)
                ? category.getRefCategory().getId()
                : category.getId();

        return productRepository.findSimilarProductsByCategory(
                refCategoryId,
                base.getId(),
                PageRequest.of(0, limit)
        );
    }

    @Override
    public List<RecommendReasonResponseDto> getRecommendationReason(Long userId, Long productId) {
        // 최근 추천 로그 조회
        UserRecommendation recentLog = userRecommendRepository.findTopByMemberIdOrderByCreatedAtDesc(userId)
                .orElse(null);
        if (recentLog == null) return Collections.emptyList();

        Product base = recentLog.getProduct();
        Product target = productRepository.findById(productId)
                .orElseThrow(() -> RecommendException.targetProductNotFound("productId=" + productId));

        List<RecommendReasonResponseDto> reasons = new ArrayList<>();

        addIfPresent(reasons, compareCategory(base, target));
        addIfPresent(reasons, compareBrand(base, target));
        addIfPresent(reasons, comparePrice(base, target));
        addIfPresent(reasons, compareName(base, target));

        return reasons;
    }

    private void addIfPresent(List<RecommendReasonResponseDto> reasons, Optional<RecommendReasonResponseDto> reasonOpt) {
        reasonOpt.ifPresent(reasons::add);
    }

    //카테고리 비교
    private Optional<RecommendReasonResponseDto> compareCategory(Product base, Product target) {
        if (base.getCategory() != null && base.getCategory().equals(target.getCategory())) {
            return Optional.of(new RecommendReasonResponseDto("category", "같은 카테고리의 상품이에요."));
        }
        return Optional.empty();
    }

    //브랜드 비교
    private Optional<RecommendReasonResponseDto> compareBrand(Product base, Product target) {
        if (base.getBrand() != null && base.getBrand().equals(target.getBrand())) {
            return Optional.of(new RecommendReasonResponseDto("brand", "같은 브랜드의 상품이에요."));
        }
        return Optional.empty();
    }

    //가격 비교
    private Optional<RecommendReasonResponseDto> comparePrice(Product base, Product target) {
        long diff = Math.abs(base.getPrice() - target.getPrice());
        if (diff < 10_000) {
            return Optional.of(new RecommendReasonResponseDto("price", "비슷한 가격대의 상품이에요."));
        } else if (diff > 30_000) {
            return Optional.of(new RecommendReasonResponseDto("price", "조금 다른 가격대의 상품이에요."));
        }
        return Optional.empty();
    }

    //상품명 유사도 비교
    private Optional<RecommendReasonResponseDto> compareName(Product base, Product target) {
        if (isNameSimilar(base.getName(), target.getName())) {
            return Optional.of(new RecommendReasonResponseDto("name", "이름이 비슷한 상품이에요."));
        }
        return Optional.empty();
    }

    @Override
    public double getRecommendationScore(Long memberId, Long productId) {
        UserActionSummary summary = getCachedUserActionSummary(memberId, productId);
        LocalDateTime now = LocalDateTime.now();

        double totalScore =
                scoreWithDecay(summary.getClickedAt(), CLICK_WEIGHT, now)
                        + scoreWithDecay(summary.getLikedAt(), LIKE_WEIGHT, now, summary.getLiked())
                        + scoreWithDecay(summary.getInCartAt(), CART_WEIGHT, now, summary.getInCart())
                        + scoreWithDecay(summary.getPurchasedAt(), PURCHASE_WEIGHT, now, summary.getPurchased());

        double maxScore = CLICK_WEIGHT + LIKE_WEIGHT + CART_WEIGHT + PURCHASE_WEIGHT;
        return (totalScore / maxScore) * 100;
    }

    private UserActionSummary getCachedUserActionSummary(Long memberId, Long productId) {
        String cacheKey = "user:action:" + memberId + ":" + productId;
        UserActionSummary cached = (UserActionSummary) redisTemplate.opsForValue().get(cacheKey);

        if (cached != null) return cached;

        UserActionSummary summary = summaryRepository.findByMemberIdAndProductId(memberId, productId)
                .orElse(new UserActionSummary(memberId, productId, 0, false, false, false));

        redisTemplate.opsForValue().set(cacheKey, summary, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        return summary;
    }

    private double scoreWithDecay(LocalDateTime actionTime, double weight, LocalDateTime now) {
        if (actionTime == null) return 0;
        long days = ChronoUnit.DAYS.between(actionTime, now);
        return weight * Math.max(MIN_DECAY, 1 - DECAY_RATE * days);
    }

    private double scoreWithDecay(LocalDateTime actionTime, double weight, LocalDateTime now, Boolean condition) {
        if (Boolean.FALSE.equals(condition)) return 0;
        return scoreWithDecay(actionTime, weight, now);
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
}