package com.space.munova.coupon.service;

import com.space.munova.IntegrationTestBase;
import com.space.munova.common.ConcurrencyTestHelper;
import com.space.munova.coupon.dto.IssueCouponRequest;
import com.space.munova.coupon.dto.RegisterCouponDetailRequest;
import com.space.munova.coupon.dto.RegisterCouponDetailResponse;
import com.space.munova.coupon.entity.CouponDetail;
import com.space.munova.coupon.exception.CouponException;
import com.space.munova.coupon.repository.CouponDetailRepository;
import com.space.munova.coupon.repository.CouponRepository;
import com.space.munova.member.entity.Member;
import com.space.munova.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("쿠폰 동시성 테스트")
class CouponServiceConcurrencyTest extends IntegrationTestBase {

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponDetailService couponDetailService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CouponDetailRepository couponDetailRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Nested
    @DisplayName("쿠폰 발급")
    class IssuedCouponTest {

        private static final Long QUANTITY_CNT = 1L;
        private static final int CONCURRENT_THREADS = 1;

        private Long couponDetailId;
        private List<Member> testMembers;

        @BeforeEach
        void setUp() {
            couponRepository.deleteAll();
            memberRepository.deleteAll();

            RegisterCouponDetailRequest request = RegisterCouponDetailRequest.builder()
                    .quantity(QUANTITY_CNT)
                    .couponName("test-couponName")
                    .couponType("FIXED")
                    .discountAmount(1000L)
                    .expiredAt(LocalDateTime.now().plusDays(10))
                    .build();

            RegisterCouponDetailResponse response = couponDetailService.registerCoupon(1L, request);
            couponDetailId = response.couponDetailId();

            testMembers = new ArrayList<>();
            for (int i = 0; i < CONCURRENT_THREADS; i++) {
                Member member = Member.createMember("user-" + i, "password", "address");
                testMembers.add(memberRepository.save(member));
            }
        }

        @Test
        @DisplayName("1인당 1매씩 쿠폰 발행이 되어야한다.")
        void issueCouponByOneToOne() throws InterruptedException {
            ConcurrencyTestHelper helper = new ConcurrencyTestHelper(CONCURRENT_THREADS);

            helper.execute(index -> {
                Member member = testMembers.get(index);
                IssueCouponRequest request = IssueCouponRequest.of(couponDetailId, member.getId());
                couponService.issueCoupon(request);
            });

            assertEquals(CONCURRENT_THREADS, helper.getSuccessCount(),
                    CONCURRENT_THREADS + "개의 쿠폰 발급이 성공해야 함");

            CouponDetail couponDetail = couponDetailRepository.findById(couponDetailId).orElseThrow();

            assertEquals(0L, couponDetail.getRemainQuantity(), "남은 쿠폰 수량이 0이어야 함");

            long issuedCouponCount = couponRepository.count();
            assertEquals(CONCURRENT_THREADS, issuedCouponCount,
                    "발급된 쿠폰 개수가 정확히 " + CONCURRENT_THREADS + "개여야 함. 현재: " + issuedCouponCount);

            for (int i = 0; i < CONCURRENT_THREADS; i++) {
                assertTrue(
                        couponRepository.existsByMemberIdAndCouponDetailId(testMembers.get(i).getId(), couponDetailId),
                        "멤버 " + i + "가 쿠폰을 발급받아야 함"
                );
            }
        }

        @Test
        @DisplayName("하나의 쿠폰을 동시에 발급받았을 경우 남은 수량이 0미만으로 떨어지지 않아야 함")
        void remainQuantityShouldNotBeNegative() throws InterruptedException {
            couponRepository.deleteAll();
            couponDetailRepository.deleteAll();

            RegisterCouponDetailRequest request = RegisterCouponDetailRequest.builder()
                    .quantity(10L)
                    .couponName("limited-coupon")
                    .couponType("FIXED")
                    .discountAmount(1000L)
                    .expiredAt(LocalDateTime.now().plusDays(10))
                    .build();

            RegisterCouponDetailResponse registerCouponDetailResponse
                    = couponDetailService.registerCoupon(1L, request);
            Long limitedCouponDetailId = registerCouponDetailResponse.couponDetailId();

            int threadCount = 50;
            ConcurrencyTestHelper helper = new ConcurrencyTestHelper(threadCount);

            helper.execute(
                    index -> {
                        Member member = testMembers.get(index);
                        IssueCouponRequest request2 = IssueCouponRequest.of(limitedCouponDetailId, member.getId());
                        couponService.issueCoupon(request2);
                    },
                    e -> e instanceof CouponException // 쿠폰 소진 예외는 정상
            );

            CouponDetail couponDetail = couponDetailRepository.findById(limitedCouponDetailId).orElseThrow();

            assertFalse(couponDetail.getRemainQuantity() < 0,
                    "남은 쿠폰 수량이 음수가 되면 안 됨. 현재 수량: " + couponDetail.getRemainQuantity());

            long issuedCount = couponRepository.count();
            long expectedTotal = couponDetail.getRemainQuantity() + issuedCount;
            assertEquals(10L, expectedTotal,
                    "발급된 쿠폰 수 + 남은 수량이 원래 수량(10)과 같아야 함. 현재 발급: " + issuedCount + ", 남은: " + couponDetail.getRemainQuantity());

            assertTrue(issuedCount <= 10L,
                    "발급된 쿠폰이 원래 수량(10)을 초과하면 안 됨. 발급: " + issuedCount);
        }

        @Test
        @DisplayName("같은 유저가 같은 쿠폰을 두 번 발급받을 수 없어야 함")
        void cannotIssueSameCouponTwiceForSameUser() throws InterruptedException {
            Member testMember = testMembers.get(0);

            IssueCouponRequest request = IssueCouponRequest.of(couponDetailId, testMember.getId());
            couponService.issueCoupon(request);

            int threadCount = 5;
            ConcurrencyTestHelper helper = new ConcurrencyTestHelper(threadCount);

            helper.execute(
                    index -> {
                        IssueCouponRequest duplicateRequest = IssueCouponRequest.of(couponDetailId, testMember.getId());
                        couponService.issueCoupon(duplicateRequest);
                    },
                    e -> e instanceof CouponException // 중복 발급 예외는 정상
            );

            assertEquals(0, helper.getSuccessCount(),
                    "추가 발급이 모두 실패해야 함");
            assertEquals(threadCount, helper.getFailureCount(),
                    "모든 재시도가 실패(중복 발급 거부)해야 함");

            long userCouponCount = couponRepository.count();
            assertEquals(1L, userCouponCount,
                    "해당 유저는 정확히 1개의 쿠폰만 소유해야 함. 현재: " + userCouponCount);
        }

    }

}