package com.space.munova.security.jwt;

import com.space.munova.member.dto.MemberRole;
import io.jsonwebtoken.Claims;
import lombok.Getter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.List;

@Getter
public class JwtAuthenticationToken extends UsernamePasswordAuthenticationToken {

    private Claims claims;

    public JwtAuthenticationToken(Object principal, Object credentials) {
        super(principal, credentials);
    }

    public JwtAuthenticationToken(
            Object principal, Object credentials, Collection<? extends GrantedAuthority> authorities, Claims claims
    ) {
        super(principal, credentials, authorities);
        this.claims = claims;
    }

    public static JwtAuthenticationToken beforeOf(String accessToken) {
        return new JwtAuthenticationToken(accessToken, "");
    }

    public static JwtAuthenticationToken afterOf(long memberId, MemberRole role, Claims claims) {
        return new JwtAuthenticationToken(memberId, "", List.of(role), claims);
    }

    public String getAccessToken() {
        return this.getPrincipal() != null ? this.getPrincipal().toString() : null;
    }

}
