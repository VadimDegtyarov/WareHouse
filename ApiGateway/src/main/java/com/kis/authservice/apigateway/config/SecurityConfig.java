package com.kis.authservice.apigateway.config;

import com.kis.authservice.apigateway.Errors.NoPopupAuthenticationEntryPoint;
import com.kis.authservice.apigateway.JWTConfiguration.JwtCookieAuthenticationFilter;

import jakarta.ws.rs.HttpMethod;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * Конфигурация безопасности API Gateway.
 * Определяет правила доступа к различным endpoint'ам на основе ролей пользователей.
 * 
 * РОЛИ:
 * - ADMIN: Полный доступ ко всем функциям системы
 * - MANAGER: Управление всеми бизнес-процессами, аналитика
 * - ANALYST: Только аналитика и отчёты (только чтение)
 * - WAREHOUSE_OPERATOR: Операции на складе (приёмка, отгрузка, перемещение)
 * - PROCUREMENT_SPECIALIST: Закупки и работа с поставщиками
 * - SALES_SPECIALIST: Продажи и работа с клиентами
 * - USER: Базовый доступ (просмотр каталога)
 */
@RequiredArgsConstructor
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    private final JwtCookieAuthenticationFilter jwtCookieAuthenticationFilter;
    private final NoPopupAuthenticationEntryPoint noPopupAuthenticationEntryPoint;

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:5173"
        ));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("X-User-Id", "X-User-Roles", "Content-Type", "Authorization", "x-requested-with"));
        config.setExposedHeaders(Arrays.asList("X-User-Id", "X-User-Roles"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .cors().and()
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        // =====================
                        // ПУБЛИЧНЫЕ ENDPOINTS
                        // =====================
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers("/auth-service/login", "/auth-service/api/v1/user/create-user").permitAll()
                        .pathMatchers("/auth-service/auth/**").permitAll()
                        .pathMatchers("/auth-service/swagger-ui/**", "/auth-service/v3/api-docs/**").permitAll()
                        // Самостоятельное назначение роли (dev/диплом) — только аутентифицированным
                        .pathMatchers("/auth-service/api/v1/user/grant-role-self").authenticated()
                        
                        // =====================
                        // АДМИНИСТРАТИВНЫЕ ENDPOINTS
                        // =====================
                        // Управление пользователями - только админ
                        .pathMatchers("/wms-application/api/v1/admin/**").hasRole("ADMIN")
                        
                        // =====================
                        // АНАЛИТИКА И ОТЧЁТЫ (DSS)
                        // =====================
                        // Доступ для админов, менеджеров и аналитиков
                        .pathMatchers("/wms-application/api/v1/analytics/**").hasAnyRole("ADMIN", "MANAGER", "ANALYST")
                        
                        // =====================
                        // СКЛАД И ИНВЕНТАРИЗАЦИЯ
                        // =====================
                        // Топология склада, зоны, ячейки
                        .pathMatchers("/wms-application/api/v1/warehouse/**").hasAnyRole("ADMIN", "MANAGER", "WAREHOUSE_OPERATOR")
                        // Перемещение товаров, корректировки
                        .pathMatchers("/wms-application/api/v1/inventory/**").hasAnyRole("ADMIN", "MANAGER", "WAREHOUSE_OPERATOR")
                        // Просмотр остатков - всем авторизованным
                        .pathMatchers(org.springframework.http.HttpMethod.GET, "/wms-application/api/v1/stock/**").authenticated()
                        // Изменение остатков - только склад
                        .pathMatchers("/wms-application/api/v1/stock/**").hasAnyRole("ADMIN", "MANAGER", "WAREHOUSE_OPERATOR")
                        
                        // =====================
                        // ЗАКУПКИ
                        // =====================
                        // Заказы на закупку
                        .pathMatchers("/wms-application/api/v1/procurement/**").hasAnyRole("ADMIN", "MANAGER", "PROCUREMENT_SPECIALIST")
                        // Поставщики (компании)
                        .pathMatchers(org.springframework.http.HttpMethod.GET, "/wms-application/api/v1/companies/**").hasAnyRole("ADMIN", "MANAGER", "PROCUREMENT_SPECIALIST", "ANALYST")
                        .pathMatchers("/wms-application/api/v1/companies/**").hasAnyRole("ADMIN", "MANAGER", "PROCUREMENT_SPECIALIST")
                        
                        // =====================
                        // ПРОДАЖИ
                        // =====================
                        // Заказы на продажу, клиенты
                        .pathMatchers("/wms-application/api/v1/sales/**").hasAnyRole("ADMIN", "MANAGER", "SALES_SPECIALIST")
                        
                        // =====================
                        // КАТАЛОГ ТОВАРОВ
                        // =====================
                        // Просмотр каталога - всем авторизованным
                        .pathMatchers(org.springframework.http.HttpMethod.GET, "/wms-application/api/v1/catalog/**").authenticated()
                        // Редактирование каталога - менеджеры и выше
                        .pathMatchers("/wms-application/api/v1/catalog/**").hasAnyRole("ADMIN", "MANAGER")
                        // Старый путь products (для совместимости)
                        .pathMatchers(org.springframework.http.HttpMethod.GET, "/wms-application/api/v1/products/**").authenticated()
                        .pathMatchers("/wms-application/api/v1/products/**").hasAnyRole("ADMIN", "MANAGER")
                        
                        // =====================
                        // ПОЛЬЗОВАТЕЛИ
                        // =====================
                        // Текущий пользователь - всем авторизованным
                        .pathMatchers("/wms-application/api/v1/users/current-user").authenticated()
                        .pathMatchers("/wms-application/api/v1/users/me").authenticated()
                        .pathMatchers("/wms-application/api/v1/users/add-info").authenticated()
                        .pathMatchers(org.springframework.http.HttpMethod.GET, "/wms-application/api/v1/users/{id}/avatar").authenticated()
                        .pathMatchers(org.springframework.http.HttpMethod.POST, "/wms-application/api/v1/users/{id}/avatar").authenticated()
                        // Управление пользователями - менеджеры и админы
                        .pathMatchers("/wms-application/api/v1/users/**").hasAnyRole("ADMIN", "MANAGER")
                        
                        // =====================
                        // УСТАРЕВШИЕ ПУТИ
                        // =====================
                        .pathMatchers("/user-service/**").hasRole("USER")
                        
                        // =====================
                        // ВСЁ ОСТАЛЬНОЕ
                        // =====================
                        .pathMatchers(org.springframework.http.HttpMethod.OPTIONS).permitAll()
                        .anyExchange().authenticated()
                )
                .addFilterBefore(jwtCookieAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .exceptionHandling(spec -> spec.authenticationEntryPoint(noPopupAuthenticationEntryPoint))
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .build();
    }

    @Bean
    public ReactiveUserDetailsService userDetailsService() {
        return username -> Mono.empty();
    }
}
