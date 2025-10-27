package com.space.munova.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.space.munova.IntegrationTestBase;
import com.space.munova.auth.dto.SignInRequest;
import com.space.munova.auth.dto.SignupRequest;
import com.space.munova.auth.repository.RefreshTokenRedisRepository;
import com.space.munova.member.entity.Member;
import com.space.munova.member.repository.MemberRepository;
import com.space.munova.security.jwt.JwtHelper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class AuthControllerTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RefreshTokenRedisRepository refreshTokenRedisRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtHelper jwtHelper;

    private static final String USER_NAME = "testuser";
    private static final String USER_PASSWORD = "password123";

    @BeforeEach
    void setUp() {
        memberRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("회원가입 API 성공")
    void signup_success() throws Exception {
        // given
        SignupRequest request = SignupRequest.of(USER_NAME, USER_PASSWORD, "");

        // when & then
        mockMvc.perform(post("/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("success"))
                .andExpect(jsonPath("$.data.username").value(USER_NAME));

        assertThat(memberRepository.existsByUsername(USER_NAME)).isTrue();
    }

    @Test
    @DisplayName("회원가입 API 실패 - 중복된 사용자명")
    void signup_fail_duplicateUsername() throws Exception {
        // given
        memberRepository.save(Member.createMember(USER_NAME, passwordEncoder.encode(USER_PASSWORD), ""));
        SignupRequest request = SignupRequest.of(USER_NAME, "password456", "");

        // when & then
        mockMvc.perform(post("/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("로그인 API 성공")
    void signIn_success() throws Exception {
        // given
        memberRepository.save(Member.createMember(USER_NAME, passwordEncoder.encode(USER_PASSWORD), ""));
        SignInRequest request = SignInRequest.of(USER_NAME, USER_PASSWORD);

        // when & then
        MvcResult result = mockMvc.perform(post("/auth/signin")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("success"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(cookie().exists(JwtHelper.REFRESH_TOKEN_COOKIE_KEY))
                .andExpect(cookie().httpOnly(JwtHelper.REFRESH_TOKEN_COOKIE_KEY, true))
                .andExpect(cookie().secure(JwtHelper.REFRESH_TOKEN_COOKIE_KEY, true))
                .andReturn();

        // Redis에 refreshToken 저장 확인
        Member member = memberRepository.findByUsername(USER_NAME).orElseThrow();
        String storedRefreshToken = refreshTokenRedisRepository.findBy(member.getId());
        assertThat(storedRefreshToken).isNotNull();

        // 쿠키 값 확인
        Cookie cookie = result.getResponse().getCookie(JwtHelper.REFRESH_TOKEN_COOKIE_KEY);
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isNotBlank();
    }

    @Test
    @DisplayName("로그인 API 실패 - 잘못된 비밀번호")
    void signIn_fail_invalidPassword() throws Exception {
        // given
        memberRepository.save(Member.createMember(USER_NAME, passwordEncoder.encode(USER_PASSWORD), ""));
        SignInRequest request = SignInRequest.of(USER_NAME, "wrongpassword");

        // when & then
        mockMvc.perform(post("/auth/signin")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("로그아웃 API 성공")
    void signOut_success() throws Exception {
        // given
        Member member = memberRepository.save(Member.createMember(USER_NAME, passwordEncoder.encode(USER_PASSWORD), ""));
        String accessToken = jwtHelper.generateAccessToken(member.getId(), member.getUsername(), member.getRole());
        String refreshToken = jwtHelper.generateRefreshToken(member.getId());
        refreshTokenRedisRepository.save(member.getId(), refreshToken, System.currentTimeMillis() + 86400000);

        // when & then
        MvcResult result = mockMvc.perform(post("/api/auth/signout")
                        .with(csrf())
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("success"))
                .andExpect(cookie().exists(JwtHelper.REFRESH_TOKEN_COOKIE_KEY))
                .andExpect(cookie().maxAge(JwtHelper.REFRESH_TOKEN_COOKIE_KEY, 0))
                .andReturn();

        // Redis에서 refreshToken 삭제 확인
        String storedRefreshToken = refreshTokenRedisRepository.findBy(member.getId());
        assertThat(storedRefreshToken).isNull();
    }

    @Test
    @DisplayName("토큰 재발급 API 성공")
    void reissueToken_success() throws Exception {
        // given
        Member member = memberRepository.save(Member.createMember(USER_NAME, passwordEncoder.encode(USER_PASSWORD), ""));
        String refreshToken = jwtHelper.generateRefreshToken(member.getId());
        long expireTime = jwtHelper.getClaims(refreshToken, io.jsonwebtoken.Claims::getExpiration).getTime();
        refreshTokenRedisRepository.save(member.getId(), refreshToken, expireTime);

        Cookie refreshTokenCookie = new Cookie(JwtHelper.REFRESH_TOKEN_COOKIE_KEY, refreshToken);

        // 재발급 시간이 너무 빨라서 토큰이 같게 나옴
        Thread.sleep(3000);

        // when & then
        MvcResult result = mockMvc.perform(post("/auth/reissue")
                        .with(csrf())
                        .cookie(refreshTokenCookie))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("success"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(cookie().exists(JwtHelper.REFRESH_TOKEN_COOKIE_KEY))
                .andReturn();

        // 새로운 refreshToken이 Redis에 저장되었는지 확인
        String newStoredRefreshToken = refreshTokenRedisRepository.findBy(member.getId());
        assertThat(newStoredRefreshToken).isNotNull();
        assertThat(newStoredRefreshToken).isNotEqualTo(refreshToken);

        // 새로운 refreshToken이 쿠키에 저장되었는지 확인
        Cookie newCookie = result.getResponse().getCookie(JwtHelper.REFRESH_TOKEN_COOKIE_KEY);
        assertThat(newCookie).isNotNull();
        assertThat(newCookie.getValue()).isNotEqualTo(refreshToken);
    }

    @Test
    @DisplayName("토큰 재발급 API 실패 - refreshToken 쿠키 없음")
    void reissueToken_fail_noCookie() throws Exception {
        // when & then
        mockMvc.perform(post("/auth/reissue")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("토큰 재발급 API 실패 - Redis에 저장된 토큰과 불일치")
    void reissueToken_fail_mismatchedToken() throws Exception {
        // given
        Member member = memberRepository.save(Member.createMember(USER_NAME, passwordEncoder.encode(USER_PASSWORD), ""));
        String validRefreshToken = jwtHelper.generateRefreshToken(member.getId());

        // 시간 차이를 두어 다른 토큰 생성
        Thread.sleep(1000);
        String invalidRefreshToken = jwtHelper.generateRefreshToken(member.getId());

        long expireTime = jwtHelper.getClaims(validRefreshToken, io.jsonwebtoken.Claims::getExpiration).getTime();
        refreshTokenRedisRepository.save(member.getId(), validRefreshToken, expireTime);

        Cookie refreshTokenCookie = new Cookie(JwtHelper.REFRESH_TOKEN_COOKIE_KEY, invalidRefreshToken);

        // when & then
        mockMvc.perform(post("/auth/reissue")
                        .with(csrf())
                        .cookie(refreshTokenCookie))
                .andDo(print())
                .andExpect(status().is4xxClientError());

        // Redis에 원래 토큰이 그대로 있는지 확인 (삭제되지 않음)
        String storedRefreshToken = refreshTokenRedisRepository.findBy(member.getId());
        assertThat(storedRefreshToken).isNotNull();
        assertThat(storedRefreshToken).isEqualTo(validRefreshToken);
    }
}
