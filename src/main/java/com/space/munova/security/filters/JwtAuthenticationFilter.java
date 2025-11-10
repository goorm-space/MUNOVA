package com.space.munova.security.filters;

import com.space.munova.security.exception.CustomAuthenticationException;
import com.space.munova.security.jwt.JwtAuthenticationToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.io.IOException;

import static com.space.munova.core.config.StaticVariables.AUTH_HEADER_PREFIX;

public class JwtAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    private static final String HEADER_PREFIX = "Bearer ";

    public JwtAuthenticationFilter(RequestMatcher matcher) {
        super(matcher);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        String accessToken = request.getHeader("Authorization");
        if (accessToken == null || !accessToken.startsWith(AUTH_HEADER_PREFIX)) {
            throw new CustomAuthenticationException("Authorization Header is Invalid!");
        }
        String accessTokenContent = accessToken.substring(AUTH_HEADER_PREFIX.length());
        JwtAuthenticationToken beforeToken = JwtAuthenticationToken.beforeOf(accessTokenContent);
        return super.getAuthenticationManager().authenticate(beforeToken);
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {
        JwtAuthenticationToken afterToken = (JwtAuthenticationToken) authResult;

        // SecurityContextHolder에 인증 사용자 넣기
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(afterToken);
        SecurityContextHolder.setContext(context);

        chain.doFilter(request, response);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        SecurityContextHolder.clearContext();
        super.unsuccessfulAuthentication(request, response, failed);
    }
}
