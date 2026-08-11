package com.saibao.invoice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 本地联调安全配置。
 * 正式接入钉钉后，应在此处替换为免登 code 校验并将用户身份写入安全上下文。
 */
@Configuration
public class SecurityConfig {

    private final FinanceSessionAuthenticationFilter financeSessionAuthenticationFilter;
    private final EmployeeSessionAuthenticationFilter employeeSessionAuthenticationFilter;

    public SecurityConfig(FinanceSessionAuthenticationFilter financeSessionAuthenticationFilter,
                          EmployeeSessionAuthenticationFilter employeeSessionAuthenticationFilter) {
        this.financeSessionAuthenticationFilter = financeSessionAuthenticationFilter;
        this.employeeSessionAuthenticationFilter = employeeSessionAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers("/api/auth/login", "/api/employee/auth/dingtalk", "/api/employee/auth/organizations",
                                "/api/employee/invoice-titles/qr/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/employee/**").hasRole("EMPLOYEE")
                        .requestMatchers("/api/admin/finance-users/**").hasRole("SUPER_ADMIN")
                        .requestMatchers("/api/admin/**", "/api/auth/me", "/api/auth/logout", "/api/auth/change-password")
                            .hasAnyRole("SUPER_ADMIN", "FINANCE")
                        .anyRequest().permitAll())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setStatus(401);
                            response.setContentType("application/json;charset=UTF-8");
                            String message = request.getRequestURI().startsWith("/api/employee/")
                                    ? "请从钉钉工作台登录后访问"
                                    : "请先登录财务管理端";
                            response.getWriter().write("{\"message\":\"" + message + "\"}");
                        })
                        .accessDeniedHandler((request, response, exception) -> {
                            response.setStatus(403);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"message\":\"没有该操作权限\"}");
                        }))
                .addFilterBefore(financeSessionAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(employeeSessionAuthenticationFilter, FinanceSessionAuthenticationFilter.class)
                .build();
    }
}
