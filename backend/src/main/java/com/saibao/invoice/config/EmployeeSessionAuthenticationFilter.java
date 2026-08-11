package com.saibao.invoice.config;

import com.saibao.invoice.service.IEmployeeAuthService;
import com.saibao.invoice.vo.DingEmployeeVO;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/** 将钉钉免登成功后保存在 HttpSession 中的员工恢复为服务端可信身份。 */
@Component
@RequiredArgsConstructor
public class EmployeeSessionAuthenticationFilter extends OncePerRequestFilter {
    public static final String SESSION_EMPLOYEE_ID = "DING_EMPLOYEE_ID";
    private final IEmployeeAuthService employeeAuthService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/employee/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            Object employeeId = session.getAttribute(SESSION_EMPLOYEE_ID);
            if (employeeId instanceof Long id) {
                try {
                    DingEmployeeVO employee = employeeAuthService.getActiveEmployee(id);
                    var authority = new SimpleGrantedAuthority("ROLE_EMPLOYEE");
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(employee, null, List.of(authority)));
                } catch (SecurityException exception) {
                    session.removeAttribute(SESSION_EMPLOYEE_ID);
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
