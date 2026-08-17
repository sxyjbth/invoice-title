package com.saibao.invoice.config;

import com.saibao.invoice.service.IFinanceAccountService;
import com.saibao.invoice.service.IFinanceSessionTokenService;
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
    public static final String SESSION_HEADER = "X-Invoice-Finance-Session";

    private final IFinanceAccountService financeAccountService;
    private final IFinanceSessionTokenService financeSessionTokenService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/employee/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String tabToken = request.getHeader(SESSION_HEADER);
        if (tabToken != null) {
            // 请求头存在（包括空值）即代表新版财务端；不得回退到浏览器标签页之间共享的 Cookie。
            authenticateAccount(financeSessionTokenService.resolveAccountId(tabToken));
        } else {
            authenticateLegacySession(request);
        }
        filterChain.doFilter(request, response);
    }

    private void authenticateLegacySession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object accountId = session.getAttribute(SESSION_ACCOUNT_ID);
            if (accountId instanceof Long id) {
                if (!authenticateAccount(id)) {
                    session.invalidate();
                }
            }
        }
    }

    private boolean authenticateAccount(Long accountId) {
        if (accountId == null) {
            return false;
        }
        try {
            FinanceAccountVO account = financeAccountService.getById(accountId);
            if (!"ENABLED".equals(account.getStatus())) {
                return false;
            }
            var authority = new SimpleGrantedAuthority("ROLE_" + account.getRoleType());
            var authentication = new UsernamePasswordAuthenticationToken(account, null, List.of(authority));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
