package com.saibao.invoice.controller;

import com.saibao.invoice.dto.CreateFinanceAccountDTO;
import com.saibao.invoice.dto.FinanceAccountPageQueryDTO;
import com.saibao.invoice.dto.ResetFinancePasswordDTO;
import com.saibao.invoice.dto.UpdateFinanceStatusDTO;
import com.saibao.invoice.service.IFinanceAccountService;
import com.saibao.invoice.vo.FinanceAccountVO;
import com.saibao.invoice.vo.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 超级管理员维护财务账号接口。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/finance-users")
@Tag(name = "超级管理员-财务账号", description = "创建财务账号及忘记密码后的管理员重置")
public class FinanceAccountController {
    private final IFinanceAccountService financeAccountService;

    @GetMapping
    @Operation(summary = "分页查询财务账号", description = "仅返回 FINANCE 角色，支持账号状态和关键字筛选")
    public PageResult<FinanceAccountVO> page(@Valid FinanceAccountPageQueryDTO query) {
        return financeAccountService.pageFinanceAccounts(query);
    }

    @PostMapping
    @Operation(summary = "创建财务账号", description = "仅超级管理员可调用；新账号角色固定为 FINANCE")
    public FinanceAccountVO create(@AuthenticationPrincipal FinanceAccountVO operator,
                                   @Valid @RequestBody CreateFinanceAccountDTO request) {
        return financeAccountService.createFinanceAccount(operator.getId(), request);
    }

    @PostMapping("/{accountId}/reset-password")
    @Operation(summary = "重置财务账号密码", description = "用于财务忘记密码后联系超级管理员处理")
    public void resetPassword(
            @AuthenticationPrincipal FinanceAccountVO operator,
            @Parameter(description = "待重置的财务账号 ID", required = true, example = "2") @PathVariable Long accountId,
            @Valid @RequestBody ResetFinancePasswordDTO request) {
        financeAccountService.resetPassword(operator.getId(), accountId, request.getNewPassword());
    }

    @PatchMapping("/{accountId}/status")
    @Operation(summary = "启用或停用财务账号", description = "停用后现有会话在下一次请求时立即失效")
    public void updateStatus(
            @AuthenticationPrincipal FinanceAccountVO operator,
            @Parameter(description = "财务账号 ID", required = true, example = "2") @PathVariable Long accountId,
            @Valid @RequestBody UpdateFinanceStatusDTO request) {
        financeAccountService.updateStatus(operator.getId(), accountId, request.getStatus());
    }
}
