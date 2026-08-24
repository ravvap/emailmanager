package com.fdic.tip.emailmanager.config;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties.Jwt;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
 import org.springframework.security.web.SecurityFilterChain;

import com.fdic.tip.emailmanager.constant.ApiPaths;

/**
 * Two layers of authorization, deliberately:
 *  1. URL-level rules below gate *which endpoints exist at all* for a caller - this is
 *     the layer the team has found most reliable in test slices (a missing/misconfigured
 *     @PreAuthorize on a new endpoint fails open; a missing URL rule fails closed by
 *     default since anyRequest().authenticated() is the fallback).
 *  2. @PreAuthorize on service methods (see DataConnectionServiceImpl) as defense-in-depth
 *     and to express business-level checks (e.g. author-specific access) that don't map
 *     cleanly to a URL pattern.
 *
 * Azure AD app roles arrive in the JWT "roles" claim; they're mapped to Spring Security
 * authorities with a ROLE_ prefix so both hasRole(...) and the SecurityRoles constants work.
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    @Profile("!local")
    public SecurityFilterChain jwtSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health/**", "/api-docs/**", "/swagger-ui/**").permitAll()

                        // Reads: admins and authors
                        .requestMatchers(HttpMethod.GET, ApiPaths.DATA_CONNECTIONS + "/**").authenticated()

                        // Writes: administrators only (also enforced via @PreAuthorize in the service layer)
                        .requestMatchers(HttpMethod.POST, ApiPaths.DATA_CONNECTIONS + "/**").authenticated()
                        .requestMatchers(HttpMethod.PUT, ApiPaths.DATA_CONNECTIONS + "/**").authenticated()
                        .requestMatchers(HttpMethod.PATCH, ApiPaths.DATA_CONNECTIONS + "/**").authenticated()
                        .requestMatchers(HttpMethod.DELETE, ApiPaths.DATA_CONNECTIONS + "/**").authenticated()

                        .anyRequest().authenticated());
 
        return http.build();
    }

    /**
     * Local-only profile: permits all requests without authentication so the team can
     * exercise the API against a local PostgreSQL instance without standing up Azure AD.
     * Never active outside the "local" Spring profile.
     */
    @Bean
    @Profile("local")
    public SecurityFilterChain localPermitAllFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

 }
