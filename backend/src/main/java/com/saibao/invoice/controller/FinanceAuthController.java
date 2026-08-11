package com.saibao.invoice.controller;

import com.saibao.invoice.config.FinanceSessionAuthenticationFilter;
import com.saibao.invoice.dto.ChangeOwnPasswordDTO;
import com.saibao.invoice.dto.FinanceLoginDTO;
import com.saibao.invoice.service.IFinanceAccountService;
import com.saibao.invoice.vo.FinanceAccountVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
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

    @PostMapping("/login")
    @Operation(summary = "账号密码登录", description = "验证成功后建立 HttpOnly 服务端会话")
    public FinanceAccountVO login(@Valid @RequestBody FinanceLoginDTO request, HttpServletRequest servletRequest) {
        FinanceAccountVO account = financeAccountService.authenticate(request.getUsername(), request.getPassword());
        HttpSession session = servletRequest.getSession(true);
        session.setAttribute(FinanceSessionAuthenticationFilter.SESSION_ACCOUNT_ID, account.getId());
        return account;
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录")
    public void logout(HttpServletRequest servletRequest) {
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
