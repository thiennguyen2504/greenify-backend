package com.webdev.greenify.config;

import com.webdev.greenify.filter.JwtBlacklistFilter;
import com.webdev.greenify.properties.JwtProperties;
import com.webdev.greenify.security.CustomAuthenticationFailureHandler;
import com.webdev.greenify.security.CustomOAuth2UserService;
import com.webdev.greenify.security.OAuth2LoginSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfiguration {

        private final CustomOAuth2UserService customOAuth2UserService;
        private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
        private final CustomAuthenticationFailureHandler customAuthenticationFailureHandler;
        private final JwtProperties jwtProperties;
        private final JwtBlacklistFilter jwtBlacklistFilter;
        private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

        @Value("${domain.url}")
        private String domainURL;

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .exceptionHandling(exception -> exception
                                                .authenticationEntryPoint(jwtAuthenticationEntryPoint))
                                .csrf(AbstractHttpConfigurer::disable)
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                                "/",
                                                                "/api/v1/auth/**",
                                                                "/error",
                                                                "/h2-console/**",
                                                                "/actuator/**",
                                                                "/oauth2/**",
                                                                "/login/oauth2/code/**",
                                                                "/swagger-ui/**",
                                                                "/v3/api-docs/**",
                                                                "/swagger-ui.html")
                                                .permitAll()
                                                .anyRequest().authenticated())
                                .addFilterBefore(jwtBlacklistFilter, BearerTokenAuthenticationFilter.class)
                                .sessionManagement(sess -> sess
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwtConfigurer -> jwtConfigurer
                                                .decoder(customJwtDecoder())
                                                .jwtAuthenticationConverter(jwtAuthenticationConverter())))
                                .oauth2Login(oauth2 -> oauth2
                                                .userInfoEndpoint(userInfo -> userInfo
                                                                .userService(customOAuth2UserService) // mapping oauth2
                                                                                                      // info to user in
                                                                                                      // db
                                                )
                                                .successHandler(oAuth2LoginSuccessHandler) // generate token or redirect
                                                .failureHandler(customAuthenticationFailureHandler))
                                .headers(headers -> headers.frameOptions(frame -> frame.disable())); // for h2
                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();

                String allowedOrigin = domainURL != null ? domainURL : "http://localhost:3000";

                configuration.setAllowedOrigins(List.of(allowedOrigin));
                configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(List.of("*"));
                configuration.setAllowCredentials(true);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }

        @Bean
        public JwtDecoder customJwtDecoder() {
                SecretKey key = new SecretKeySpec(jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8),
                                "HmacSHA256");
                return NimbusJwtDecoder.withSecretKey(key).build();
        }

        @Bean
        public JwtAuthenticationConverter jwtAuthenticationConverter() {
                JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
                grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");
                grantedAuthoritiesConverter.setAuthoritiesClaimName("roleEntities");

                JwtAuthenticationConverter authConverter = new JwtAuthenticationConverter();
                authConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
                return authConverter;
        }
}
