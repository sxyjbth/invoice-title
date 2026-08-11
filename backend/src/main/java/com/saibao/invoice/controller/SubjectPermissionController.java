package com.saibao.invoice.controller;

import com.saibao.invoice.dto.SubjectPermissionPageQueryDTO;
import com.saibao.invoice.dto.SubjectPermissionSaveDTO;
import com.saibao.invoice.service.ISubjectPermissionService;
import com.saibao.invoice.vo.PageResult;
import com.saibao.invoice.vo.SubjectPermissionVO;
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

/** 财务端主体查看权限维护接口。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/subject-permissions")
@Tag(name = "财务端-主体权限", description = "按员工或钉钉部门维护主体查看权限")
public class SubjectPermissionController {
    private final ISubjectPermissionService service;

    @GetMapping
    @Operation(summary = "分页查询主体权限")
    public PageResult<SubjectPermissionVO> page(@Valid SubjectPermissionPageQueryDTO query) {
        return service.page(query);
    }

    @PostMapping
    @Operation(summary = "新增主体授权")
    public Long create(@Valid @RequestBody SubjectPermissionSaveDTO request) {
        return service.create(request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "启用或撤销主体授权", description = "撤销后相关临时二维码立即失效")
    public void changeStatus(
            @Parameter(description = "权限主键 ID", required = true) @PathVariable Long id,
            @Parameter(description = "状态：ENABLED-有效，DISABLED-停用", required = true) @RequestParam String status,
            @Parameter(description = "操作人的钉钉用户 ID", required = true) @RequestParam String operatorUserId) {
        service.changeStatus(id, status, operatorUserId);
    }
}
