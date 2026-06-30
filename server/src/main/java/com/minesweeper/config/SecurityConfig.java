package com.minesweeper.config;

import com.minesweeper.auth.filter.AuthRateLimitFilter;
import com.minesweeper.auth.filter.JwtAuthenticationFilter;
import com.minesweeper.security.JsonAccessDeniedHandler;
import com.minesweeper.security.JsonAuthenticationEntryPoint;
import com.minesweeper.security.PingRateLimitFilter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Sicurezza: niente sessione server, JWT, CORS, limiti su login. */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") String allowedOrigins) {
        List<String> origins = Arrays.stream(allowedOrigins.split("\\s*,\\s*"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtFilter,
            PingRateLimitFilter pingRateLimitFilter,
            AuthRateLimitFilter rateLimitFilter,
            JsonAuthenticationEntryPoint entryPoint,
            JsonAccessDeniedHandler accessDenied) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers
                        .xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; frame-ancestors 'none'"))
                        .frameOptions(frame -> frame.deny())
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)))
                .sessionManagement(
                        s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDenied))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/login", "/auth/register", "/auth/refresh")
                        .permitAll()
                        .requestMatchers("/api/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/score/leaderboard").permitAll()
                        .requestMatchers(HttpMethod.GET, "/auth/user").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/auth/change-password")
                        .authenticated()
                        .requestMatchers(HttpMethod.POST, "/auth/logout").authenticated()
                        .requestMatchers(HttpMethod.POST, "/score/save").authenticated()
                        .requestMatchers(HttpMethod.GET, "/score/user").authenticated()
                        .anyRequest()
                        .denyAll())
                .addFilterBefore(
                        pingRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(
                        rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(
                        jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /** Aggiunge header di sicurezza mancanti (X-Content-Type-Options, Referrer-Policy, Permissions-Policy). */
    @Bean
    public FilterRegistrationBean<OncePerRequestFilter> securityHeadersFilter() {
        OncePerRequestFilter filter = new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(@org.springframework.lang.NonNull HttpServletRequest request,
                    @org.springframework.lang.NonNull HttpServletResponse response,
                    @org.springframework.lang.NonNull FilterChain filterChain)
                    throws ServletException, IOException {
                response.setHeader("X-Content-Type-Options", "nosniff");
                response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
                response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
                filterChain.doFilter(request, response);
            }
        };
        FilterRegistrationBean<OncePerRequestFilter> bean = new FilterRegistrationBean<>(filter);
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}
