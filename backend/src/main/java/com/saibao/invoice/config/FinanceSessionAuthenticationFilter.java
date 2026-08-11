package com.saibao.invoice.config;

import com.saibao.invoice.service.IFinanceAccountService;
import com.saibao.invoice.vo.FinanceAccountVO;
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

/** 将网页财务端 HttpSession 中的账号恢复为 Spring Security 身份。 */
@Component
@RequiredArgsConstructor
public class FinanceSessionAuthenticationFilter extends OncePerRequestFilter {
    public static final String SESSION_ACCOUNT_ID = "FINANCE_ACCOUNT_ID";

    private final IFinanceAccountService financeAccountService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/employee/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            Object accountId = session.getAttribute(SESSION_ACCOUNT_ID);
            if (accountId instanceof Long id) {
                try {
                    FinanceAccountVO account = financeAccountService.getById(id);
                    if ("ENABLED".equals(account.getStatus())) {
                        var authority = new SimpleGrantedAuthority("ROLE_" + account.getRoleType());
                        var authentication = new UsernamePasswordAuthenticationToken(account, null, List.of(authority));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    } else {
                        session.invalidate();
                    }
                } catch (IllegalArgumentException exception) {
                    session.invalidate();
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
