package com.space.munova.security.jwt;

import com.space.munova.member.dto.MemberRole;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationProvider implements AuthenticationProvider {

    private final JwtHelper jwtHelper;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        JwtAuthenticationToken beforeToken = (JwtAuthenticationToken) authentication;
        String accessToken = beforeToken.getAccessToken();

        // accessToken 검증
        jwtHelper.validateJwt(accessToken);

        // 인증객체 생성 후 반환
        long memberId = Long.parseLong(jwtHelper.getClaims(accessToken, Claims::getSubject));
        String role = jwtHelper.getClaims(accessToken, claims -> claims.get("authorities")).toString();
        MemberRole parseRole = MemberRole.valueOf(role);

        return JwtAuthenticationToken.afterOf(memberId, parseRole);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return JwtAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
