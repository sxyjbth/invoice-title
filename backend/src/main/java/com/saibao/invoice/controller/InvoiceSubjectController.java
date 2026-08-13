package com.saibao.invoice.controller;

import com.saibao.invoice.dto.InvoiceSubjectSaveDTO;
import com.saibao.invoice.dto.SubjectPageQueryDTO;
import com.saibao.invoice.dto.SubjectTitleBindingDTO;
import com.saibao.invoice.service.IInvoiceSubjectService;
import com.saibao.invoice.vo.InvoiceSubjectVO;
import com.saibao.invoice.vo.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

/** 财务端主体维护接口。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/subjects")
@Tag(name = "财务端-主体管理", description = "展示主体的分页查询、新增与启停")
public class InvoiceSubjectController {
    private final IInvoiceSubjectService service;

    @GetMapping
    @Operation(summary = "分页查询主体")
    public PageResult<InvoiceSubjectVO> page(@Valid SubjectPageQueryDTO query) {
        return service.page(query);
    }

    @PostMapping
    @Operation(summary = "新增展示主体")
    public Long create(@Valid @RequestBody InvoiceSubjectSaveDTO request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑展示主体")
    public void update(
            @Parameter(description = "主体主键 ID", required = true) @PathVariable Long id,
            @Valid @RequestBody InvoiceSubjectSaveDTO request) {
        service.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "启用或停用主体", description = "停用后员工端抬头和已签发二维码将立即失效")
    public void changeStatus(
            @Parameter(description = "主体主键 ID", required = true) @PathVariable Long id,
            @Parameter(description = "状态：ENABLED-启用，DISABLED-停用", required = true) @RequestParam String status,
            @Parameter(description = "操作人的钉钉用户 ID", required = true) @RequestParam String operatorUserId) {
        service.changeStatus(id, status, operatorUserId);
    }

    @PutMapping("/{id}/title-binding")
    @Operation(summary = "为主体绑定发票抬头", description = "一个主体仅保留一条抬头绑定关系；再次绑定时替换原关系")
    public void bindTitle(
            @Parameter(description = "主体主键 ID", required = true) @PathVariable Long id,
            @Valid @RequestBody SubjectTitleBindingDTO request) {
        service.bindTitle(id, request);
    }
}
