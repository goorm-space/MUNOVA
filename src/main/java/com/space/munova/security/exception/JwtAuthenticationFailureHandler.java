package com.space.munova.security.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        String errorCode = "AUTH_FAILED";
        if (exception instanceof CustomAuthenticationException customException) {
            errorCode = customException.getErrorCode();
        }

        Map<String, Object> body = new HashMap<>();
        body.put("statusCode", HttpServletResponse.SC_UNAUTHORIZED);
        body.put("code", errorCode);
        body.put("message", exception.getMessage());
        body.put("data", null);

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
