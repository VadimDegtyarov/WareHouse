package com.kis.wmsapplication.modules.userModule.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Фильтр для извлечения информации о пользователе из заголовков, добавленных Gateway
 * Gateway уже проверил JWT токен и добавил заголовки X-User-Id и X-User-Roles
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String userId = request.getHeader("X-User-Id");
        String userRoles = request.getHeader("X-User-Roles");
        

        if (userId != null && !userId.isEmpty()) {
            try {
                List<SimpleGrantedAuthority> authorities = Collections.emptyList();
                
                if (userRoles != null && !userRoles.isEmpty()) {
                    authorities = java.util.Arrays.stream(userRoles.split(","))
                            .map(String::trim)
                            .filter(role -> !role.isEmpty())
                            .map(role -> {
                                String roleName = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                                return new SimpleGrantedAuthority(roleName);
                            })
                            .collect(Collectors.toList());
                } else {

                    authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_AUTHENTICATED"));
                }
                
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Authenticated user: {} with roles: {}", userId, authorities);
            } catch (Exception e) {
                log.error("Error setting authentication: {}", e.getMessage(), e);
            }
        } else {

            try {
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken("gateway-authenticated", null, 
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_AUTHENTICATED")));
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("No X-User-Id header, but request passed Gateway - setting authenticated");
            } catch (Exception e) {
                log.error("Error setting gateway authentication: {}", e.getMessage(), e);
            }
        }
        
        filterChain.doFilter(request, response);
    }
}
