package com.saibao.invoice.controller;

import com.saibao.invoice.dto.AllEmployeeVisibleUpdateDTO;
import com.saibao.invoice.dto.SubjectPermissionProfileSaveDTO;
import com.saibao.invoice.service.ISubjectPermissionService;
import com.saibao.invoice.vo.FinanceAccountVO;
import com.saibao.invoice.vo.SubjectPermissionProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 财务端按主体整体配置查看权限。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/subjects")
@Tag(name = "财务端-主体查看权限", description = "从钉钉员工和部门目录选择授权对象，员工规则优先于部门规则")
public class SubjectPermissionProfileController {
    private final ISubjectPermissionService service;

    @GetMapping("/{subjectId}/permission-profile")
    @Operation(summary = "查询主体权限配置", description = "返回全员开关、部门授权、员工允许/拒绝覆盖规则和实际可见人数")
    public SubjectPermissionProfileVO getProfile(
            @Parameter(description = "主体主键 ID", required = true) @PathVariable Long subjectId) {
        return service.getProfile(subjectId);
    }

    @PutMapping("/{subjectId}/permission-profile")
    @Operation(summary = "保存主体权限配置", description = "部门和员工均使用通讯录目录 ID，服务端负责解析钉钉对象 ID；员工允许/拒绝规则优先于部门授权")
    public SubjectPermissionProfileVO saveProfile(
            @Parameter(description = "主体主键 ID", required = true) @PathVariable Long subjectId,
            @Valid @RequestBody SubjectPermissionProfileSaveDTO request,
            @AuthenticationPrincipal FinanceAccountVO account) {
        return service.saveProfile(subjectId, request, account.getUsername());
    }

    @PatchMapping("/{subjectId}/permission-profile/all-employee-visible")
    @Operation(
            summary = "即时更新全员可见开关",
            description = "仅更新全员可见状态并返回最新权限配置，不修改部门授权和员工允许/拒绝规则")
    public SubjectPermissionProfileVO updateAllEmployeeVisible(
            @Parameter(description = "主体主键 ID", required = true) @PathVariable Long subjectId,
            @Valid @RequestBody AllEmployeeVisibleUpdateDTO request,
            @AuthenticationPrincipal FinanceAccountVO account) {
        return service.updateAllEmployeeVisible(
                subjectId, Boolean.TRUE.equals(request.getAllEmployeeVisible()), account.getUsername());
    }
}
