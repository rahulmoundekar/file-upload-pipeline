package com.rahul.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityProperties properties) throws Exception {
        http.csrf(c -> c.disable()).sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        if (!properties.enabled()) {
            http.authorizeHttpRequests(a -> a.anyRequest().permitAll());
        } else {
            http.authorizeHttpRequests(a -> a.requestMatchers("/actuator/health/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll().anyRequest().authenticated());
            http.addFilterBefore(new ApiTokenFilter(properties), UsernamePasswordAuthenticationFilter.class);
        }
        return http.build();
    }

    private static final class ApiTokenFilter extends OncePerRequestFilter {
        private final SecurityProperties properties;
        private ApiTokenFilter(SecurityProperties properties) { this.properties = properties; }
        @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
            String authorization = request.getHeader("Authorization");
            if (authorization != null && authorization.startsWith("Bearer ") && authorization.substring(7).equals(properties.token())) {
                var authentication = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("api", null, java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")));
                org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            chain.doFilter(request, response);
        }
    }
}
