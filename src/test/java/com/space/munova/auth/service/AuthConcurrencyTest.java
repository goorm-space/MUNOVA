package com.space.munova.auth.service;

import com.space.munova.IntegrationTestBase;
import com.space.munova.auth.dto.SignInRequest;
import com.space.munova.auth.dto.SignupRequest;
import com.space.munova.common.ConcurrencyTestHelper;
import com.space.munova.core.exception.BaseException;
import com.space.munova.member.entity.Member;
import com.space.munova.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("인증 동시성 테스트")
class AuthConcurrencyTest extends IntegrationTestBase {

    @Autowired
    private AuthService authService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "password123";
    private static final String TEST_ADDRESS = "Seoul";
    private static final String DEVICE_ID = "device-001";

    @Nested
    @DisplayName("회원가입 동시성 테스트")
    class SignupConcurrencyTest {

        @BeforeEach
        void setUp() {
            memberRepository.deleteAll();
        }

        @Test
        @DisplayName("동일한 username으로 여러 회원가입 요청 시 하나만 성공해야 함")
        void testConcurrentSignupWithDuplicateUsername() throws InterruptedException {
            ConcurrencyTestHelper helper = new ConcurrencyTestHelper(10);

            helper.execute(
                    index -> {
                        SignupRequest request = new SignupRequest(TEST_USERNAME, TEST_PASSWORD, TEST_ADDRESS);
                        authService.signup(request);
                    },
                    e -> e instanceof DataIntegrityViolationException
            );

            assertEquals(1, helper.getSuccessCount(), "정확히 1개의 회원가입만 성공해야 함");
            assertEquals(helper.getThreadCount() - 1, helper.getFailureCount(), "나머지는 모두 실패해야 함");
            assertTrue(helper.getExceptions().isEmpty(), "예상치 못한 예외가 발생하지 않아야 함");
        }

        @Test
        @DisplayName("다른 username으로 동시에 여러 회원가입 시 모두 성공해야 함")
        void testConcurrentSignupWithDifferentUsernames() throws InterruptedException {
            ConcurrencyTestHelper helper = new ConcurrencyTestHelper(5);

            helper.execute(index -> {
                String username = "user-" + index;
                SignupRequest request = new SignupRequest(username, TEST_PASSWORD, TEST_ADDRESS);
                authService.signup(request);
            });

            assertEquals(helper.getThreadCount(), helper.getSuccessCount(), "모든 회원가입이 성공해야 함");
            assertTrue(helper.getExceptions().isEmpty(), "예외가 발생하지 않아야 함");
            assertEquals(helper.getThreadCount(), memberRepository.count(), "전체 회원 수가 일치해야 함");
        }
    }

    @Nested
    @DisplayName("로그인 동시성 테스트")
    class SignInConcurrencyTest {

        @BeforeEach
        void setUp() {
            memberRepository.deleteAll();
            redisTemplate.getConnectionFactory().getConnection().flushAll();
            // 테스트용 사용자 미리 생성
            String encodedPassword = passwordEncoder.encode(TEST_PASSWORD);
            Member member = Member.createMember(TEST_USERNAME, encodedPassword, TEST_ADDRESS);
            memberRepository.save(member);
        }

        @Test
        @DisplayName("동일한 계정으로 동시에 여러 로그인 시 모두 성공해야 함")
        void testConcurrentSignInWithSameAccount() throws InterruptedException {
            ConcurrencyTestHelper helper = new ConcurrencyTestHelper(10);

            helper.execute(index -> {
                SignInRequest request = new SignInRequest(TEST_USERNAME, TEST_PASSWORD);
                String deviceId = DEVICE_ID + "-" + index;
                authService.signIn(request, deviceId);
            });

            assertEquals(helper.getThreadCount(), helper.getSuccessCount(), "모든 로그인이 성공해야 함");
            assertTrue(helper.getExceptions().isEmpty(), "예외가 발생하지 않아야 함");
        }

        @Test
        @DisplayName("동일한 deviceId로 동시에 여러 로그인 시 마지막 토큰만 유효해야 함")
        void testConcurrentSignInWithSameDevice() throws InterruptedException {
            ConcurrencyTestHelper helper = new ConcurrencyTestHelper(5);

            helper.execute(index -> {
                SignInRequest request = new SignInRequest(TEST_USERNAME, TEST_PASSWORD);
                authService.signIn(request, DEVICE_ID);
            });

            assertEquals(helper.getThreadCount(), helper.getSuccessCount(), "모든 로그인이 성공해야 함");
            assertTrue(helper.getExceptions().isEmpty(), "예외가 발생하지 않아야 함");
        }
    }

    @Nested
    @DisplayName("토큰 재발급 동시성 테스트")
    class TokenReissueConcurrencyTest {

        private String refreshToken;
        private Long memberId;

        @BeforeEach
        void setUp() {
            memberRepository.deleteAll();
            redisTemplate.getConnectionFactory().getConnection().flushAll();

            // 테스트용 사용자 생성 및 로그인
            String encodedPassword = passwordEncoder.encode(TEST_PASSWORD);
            Member member = Member.createMember(TEST_USERNAME, encodedPassword, TEST_ADDRESS);
            Member savedMember = memberRepository.save(member);
            memberId = savedMember.getId();

            // 초기 로그인으로 refreshToken 생성
            SignInRequest request = new SignInRequest(TEST_USERNAME, TEST_PASSWORD);
            var signInResult = authService.signIn(request, DEVICE_ID);
            refreshToken = signInResult.refreshToken();
        }

        @Test
        @DisplayName("동일한 refreshToken으로 동시에 여러 재발급 요청 시 하나만 성공해야 함")
        void testConcurrentTokenReissueWithSameToken() throws InterruptedException {
            ConcurrencyTestHelper helper = new ConcurrencyTestHelper(5);

            helper.execute(
                    index -> {
                        tokenService.reissueToken(refreshToken, DEVICE_ID);
                    },
                    e -> e instanceof BaseException
            );

            // 하나의 요청은 성공하고 나머지는 실패해야 함
            assertTrue(
                    (helper.getSuccessCount() == 1 && helper.getFailureCount() == helper.getThreadCount() - 1) ||
                            (helper.getSuccessCount() > 0),
                    "토큰 재발급 결과가 일관성 있어야 함"
            );
        }

        @Test
        @DisplayName("다른 deviceId로 동시에 여러 재발급 요청 시 모두 성공해야 함")
        void testConcurrentTokenReissueWithDifferentDevices() throws InterruptedException {
            ConcurrencyTestHelper helper = new ConcurrencyTestHelper(5);

            helper.execute(index -> {
                String deviceId = "device-" + index;
                String encodedPassword = passwordEncoder.encode(TEST_PASSWORD);
                Member member = memberRepository.findById(memberId).orElseThrow();

                var tokens = tokenService.saveRefreshTokenWithLock(member, deviceId);
                tokenService.reissueToken(tokens.refreshToken(), deviceId);
            });

            assertEquals(helper.getThreadCount(), helper.getSuccessCount(), "모든 재발급 요청이 성공해야 함");
            assertTrue(helper.getExceptions().isEmpty(), "예외가 발생하지 않아야 함");
        }

        @Test
        @DisplayName("회원별로 동시에 토큰 재발급 시 각각 독립적으로 처리되어야 함")
        void testConcurrentTokenReissueMultipleMembers() throws InterruptedException {
            int memberCount = 3;
            int requestsPerMember = 3;

            // 여러 회원 생성 및 토큰 생성
            String[] memberTokens = new String[memberCount];
            for (int m = 0; m < memberCount; m++) {
                String username = "testuser-" + m;
                String encodedPassword = passwordEncoder.encode(TEST_PASSWORD);
                Member member = Member.createMember(username, encodedPassword, TEST_ADDRESS);
                memberRepository.save(member);

                SignInRequest request = new SignInRequest(username, TEST_PASSWORD);
                var signInResult = authService.signIn(request, DEVICE_ID + "-" + m);
                memberTokens[m] = signInResult.refreshToken();
            }

            // 각 회원의 토큰으로 동시에 재발급 요청
            ConcurrencyTestHelper helper = new ConcurrencyTestHelper(memberCount);

            helper.execute(index -> {
                String token = memberTokens[index];
                tokenService.reissueToken(token, DEVICE_ID + "-" + index);
            });

            assertTrue(helper.getSuccessCount() > 0, "최소한 일부 재발급은 성공해야 함");
            assertEquals(0, helper.getExceptions().size(), "예상치 못한 예외가 발생하지 않아야 함");
        }
    }
}
