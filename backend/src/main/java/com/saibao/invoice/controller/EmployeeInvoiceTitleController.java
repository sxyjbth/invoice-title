package com.saibao.invoice.controller;

import com.saibao.invoice.dto.EmployeeInvoiceTitlePageQueryDTO;
import com.saibao.invoice.service.IEmployeeInvoiceTitleService;
import com.saibao.invoice.service.IQrTokenService;
import com.saibao.invoice.vo.InvoiceTitleVO;
import com.saibao.invoice.vo.InvoiceTitleVersionVO;
import com.saibao.invoice.vo.PageResult;
import com.saibao.invoice.vo.QrTokenVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.saibao.invoice.vo.DingEmployeeVO;
import org.springframework.web.bind.annotation.RestController;

/** 钉钉员工端抬头展示与临时二维码接口。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/employee/invoice-titles")
@Tag(name = "员工端-发票抬头", description = "按主体权限查看抬头并生成十分钟临时二维码")
public class EmployeeInvoiceTitleController {

    private final IEmployeeInvoiceTitleService employeeInvoiceTitleService;
    private final IQrTokenService qrTokenService;

    @GetMapping
    @Operation(summary = "分页查询员工有权查看的抬头")
    public PageResult<InvoiceTitleVO> page(@Valid EmployeeInvoiceTitlePageQueryDTO query,
                                           @AuthenticationPrincipal DingEmployeeVO employee) {
        return employeeInvoiceTitleService.pageAuthorized(query, employee.getId());
    }

    @PostMapping("/{titleId}/qr-token")
    @Operation(summary = "生成抬头临时二维码令牌", description = "令牌固定指向当前发布版本并在十分钟后过期")
    public QrTokenVO createQrToken(
            @Parameter(description = "抬头主键 ID", required = true, example = "1") @PathVariable Long titleId,
            @AuthenticationPrincipal DingEmployeeVO employee) {
        return qrTokenService.create(titleId, employee.getId());
    }

    @GetMapping("/qr/{token}")
    @Operation(summary = "解析临时二维码", description = "返回令牌创建时对应的已发布版本快照；抬头、主体或权限停用后立即失效")
    public InvoiceTitleVersionVO resolveQrToken(
            @Parameter(description = "二维码随机令牌", required = true) @PathVariable String token) {
        return qrTokenService.resolve(token);
    }
}
