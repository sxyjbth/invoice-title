package com.saibao.invoice.controller;

import com.saibao.invoice.dto.InvoiceTitlePageQueryDTO;
import com.saibao.invoice.dto.InvoiceTitleSaveDTO;
import com.saibao.invoice.dto.RestoreVersionDTO;
import com.saibao.invoice.dto.PageQueryDTO;
import com.saibao.invoice.service.IInvoiceTitleService;
import com.saibao.invoice.service.IInvoiceTitleVersionService;
import com.saibao.invoice.vo.InvoiceTitleVO;
import com.saibao.invoice.vo.PageResult;
import com.saibao.invoice.vo.FinanceAccountVO;
import com.saibao.invoice.vo.InvoiceTitleVersionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 财务端发票抬头与版本接口。 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/invoice-titles")
@Tag(name = "财务端-发票抬头", description = "发票抬头分页查询、详情及历史版本恢复")
public class InvoiceTitleController {

    private final IInvoiceTitleService invoiceTitleService;
    private final IInvoiceTitleVersionService versionService;

    @GetMapping
    @Operation(summary = "分页查询发票抬头", description = "支持按关键字和已发布、草稿、已停用状态进行服务端分页筛选")
    public PageResult<InvoiceTitleVO> page(@Valid InvoiceTitlePageQueryDTO query) {
        return invoiceTitleService.page(query);
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询发票抬头详情")
    public InvoiceTitleVO detail(
            @Parameter(description = "抬头主键 ID", required = true, example = "1") @PathVariable Long id) {
        return invoiceTitleService.getById(id);
    }

    @PostMapping
    @Operation(summary = "新增发票抬头", description = "可保存草稿或直接发布，并同步创建不可变版本快照")
    public Long create(@Valid @RequestBody InvoiceTitleSaveDTO request,
                       @AuthenticationPrincipal FinanceAccountVO account) {
        return invoiceTitleService.create(request, account.getUsername());
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑发票抬头", description = "保存后创建新版本；发布时更新员工端当前展示版本")
    public void update(
            @Parameter(description = "抬头主键 ID", required = true) @PathVariable Long id,
            @Valid @RequestBody InvoiceTitleSaveDTO request,
            @AuthenticationPrincipal FinanceAccountVO account) {
        invoiceTitleService.update(id, request, account.getUsername());
    }

    @PostMapping("/{titleId}/versions/{versionId}/restore")
    @Operation(summary = "恢复历史版本", description = "复制所选历史快照并创建一个新草稿，不覆盖当前发布版本")
    public Long restoreVersion(
            @Parameter(description = "抬头主键 ID", required = true, example = "1") @PathVariable Long titleId,
            @Parameter(description = "来源历史版本 ID", required = true, example = "1") @PathVariable Long versionId,
            @Valid @RequestBody RestoreVersionDTO request) {
        return versionService.restoreAsDraft(titleId, versionId, request.getOperatorUserId());
    }

    @GetMapping("/{titleId}/versions")
    @Operation(summary = "分页查询抬头历史版本", description = "按版本号倒序返回不可变快照，所有历史版本列表使用服务端分页")
    public PageResult<InvoiceTitleVersionVO> versions(
            @Parameter(description = "抬头主键 ID", required = true) @PathVariable Long titleId,
            @Valid PageQueryDTO query) {
        return versionService.page(titleId, query);
    }
}
