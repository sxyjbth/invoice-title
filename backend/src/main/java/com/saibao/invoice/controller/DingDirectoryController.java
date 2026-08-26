package com.saibao.invoice.controller;

import com.saibao.invoice.dto.DepartmentDirectoryPageQueryDTO;
import com.saibao.invoice.dto.EmployeeDirectoryPageQueryDTO;
import com.saibao.invoice.dto.EmployeeSelectionResolveDTO;
import com.saibao.invoice.service.IDingDirectoryService;
import com.saibao.invoice.service.IDingDirectorySyncService;
import com.saibao.invoice.vo.DingDepartmentVO;
import com.saibao.invoice.vo.DingEmployeeVO;
import com.saibao.invoice.vo.DingOrganizationVO;
import com.saibao.invoice.vo.PageResult;
import com.saibao.invoice.vo.DingDirectorySyncResultVO;
import com.saibao.invoice.vo.FinanceAccountVO;
import com.saibao.invoice.vo.EmployeeSelectionResolveVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;

/** 财务端员工和部门选择器使用的钉钉通讯录接口。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/directory")
@Tag(name = "财务端-钉钉通讯录", description = "分页查询已同步的员工和部门，财务无需手工填写钉钉 ID")
public class DingDirectoryController {
    private final IDingDirectoryService service;
    private final IDingDirectorySyncService syncService;

    @GetMapping("/organizations")
    @Operation(summary = "查询通讯录企业选项", description = "返回已同步且包含启用部门的企业，用于部门授权企业筛选")
    public List<DingOrganizationVO> organizations() {
        return service.listOrganizations();
    }

    @GetMapping("/employees")
    @Operation(summary = "分页查询员工", description = "支持按企业、直接任职部门、姓名、工号、部门名称和手机号查询；传入主体 ID 后可按最终查看权限筛选")
    public PageResult<DingEmployeeVO> employees(@Valid @ParameterObject EmployeeDirectoryPageQueryDTO query) {
        return service.pageEmployees(query);
    }

    @GetMapping("/departments")
    @Operation(summary = "分页查询部门", description = "支持按企业和部门名称查询，返回钉钉部门 ID 和直接在职员工数")
    public PageResult<DingDepartmentVO> departments(@Valid @ParameterObject DepartmentDirectoryPageQueryDTO query) {
        return service.pageDepartments(query);
    }

    @PostMapping("/employee-selections/resolve")
    @Operation(summary = "批量解析已选员工", description = "将选中的企业、部门和员工合并为去重后的在职员工集合，并按钉钉企业分组返回")
    public EmployeeSelectionResolveVO resolveEmployeeSelections(
            @Valid @RequestBody EmployeeSelectionResolveDTO request) {
        return service.resolveEmployeeSelections(request);
    }

    @PostMapping("/sync")
    @Operation(summary = "手动同步钉钉通讯录", description = "立即从钉钉全量同步部门、员工和任职状态；同一时间只允许一个同步任务执行")
    public DingDirectorySyncResultVO synchronize(@AuthenticationPrincipal FinanceAccountVO account) {
        return syncService.synchronize("MANUAL", account.getUsername());
    }
}
