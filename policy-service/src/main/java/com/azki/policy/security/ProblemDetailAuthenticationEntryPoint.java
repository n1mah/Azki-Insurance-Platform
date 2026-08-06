package com.azki.policy.security;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/problem+json");
        response.getWriter().write("""
                {
                    "title": "Unauthorized",
                    "status": 401,
                    "detail": "Missing or invalid authentication token",
                    "instance": "%s"
                }
                """.formatted(request.getRequestURI()));
    }

}