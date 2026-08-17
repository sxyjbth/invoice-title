package com.saibao.invoice.controller;

import com.saibao.invoice.config.FinanceSessionAuthenticationFilter;
import com.saibao.invoice.dto.ChangeOwnPasswordDTO;
import com.saibao.invoice.dto.FinanceLoginDTO;
import com.saibao.invoice.service.IFinanceAccountService;
import com.saibao.invoice.service.IFinanceSessionTokenService;
import com.saibao.invoice.vo.FinanceAccountVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 独立网页财务端登录会话与本人密码接口。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "财务端-登录认证", description = "账号密码登录、退出、查询当前账号及修改本人密码")
public class FinanceAuthController {
    private final IFinanceAccountService financeAccountService;
    private final IFinanceSessionTokenService financeSessionTokenService;

    @PostMapping("/login")
    @Operation(summary = "账号密码登录", description = "验证成功后通过响应头返回当前浏览器页签的独立会话令牌；令牌有效期为 8 小时")
    public FinanceAccountVO login(@Valid @RequestBody FinanceLoginDTO request,
                                  HttpServletRequest servletRequest,
                                  HttpServletResponse servletResponse) {
        FinanceAccountVO account = financeAccountService.authenticate(request.getUsername(), request.getPassword());
        String token = financeSessionTokenService.create(account.getId());
        servletResponse.setHeader(FinanceSessionAuthenticationFilter.SESSION_HEADER, token);
        // 保留旧 Cookie 会话，兼容尚未升级的调用方；新版财务端始终优先使用页签令牌。
        HttpSession session = servletRequest.getSession(true);
        session.setAttribute(FinanceSessionAuthenticationFilter.SESSION_ACCOUNT_ID, account.getId());
        return account;
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录")
    public void logout(HttpServletRequest servletRequest) {
        String tabToken = servletRequest.getHeader(FinanceSessionAuthenticationFilter.SESSION_HEADER);
        if (tabToken != null) {
            // 新版财务端只撤销当前标签页令牌，不能使同一浏览器的其他标签页退出。
            financeSessionTokenService.invalidate(tabToken);
            return;
        }
        HttpSession session = servletRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    @GetMapping("/me")
    @Operation(summary = "查询当前登录账号")
    public FinanceAccountVO me(@AuthenticationPrincipal FinanceAccountVO account) {
        return account;
    }

    @PostMapping("/change-password")
    @Operation(summary = "修改本人密码", description = "财务和超级管理员均须先校验当前密码")
    public void changePassword(@AuthenticationPrincipal FinanceAccountVO account,
                               @Valid @RequestBody ChangeOwnPasswordDTO request) {
        financeAccountService.changeOwnPassword(account.getId(), request.getCurrentPassword(), request.getNewPassword());
    }
}
