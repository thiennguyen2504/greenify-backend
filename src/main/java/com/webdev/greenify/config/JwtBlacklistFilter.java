package com.webdev.greenify.config;

import com.webdev.greenify.auth.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtBlacklistFilter extends OncePerRequestFilter {

    private final TokenBlacklistService tokenBlacklistService;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String accessToken = authHeader.substring(7);
                if (tokenBlacklistService.isAccessTokenBlacklisted(accessToken)) {
                    throw new BadCredentialsException("Access token đã bị thu hồi");
                }
            }
            filterChain.doFilter(request, response);
        } catch (AuthenticationException e) {
            jwtAuthenticationEntryPoint.commence(request, response, e);
        }
    }
}
