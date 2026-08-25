package com.saibao.invoice.service.impl;

import com.saibao.invoice.domain.DingDepartment;
import com.saibao.invoice.domain.DingEmployee;
import com.saibao.invoice.dto.DepartmentDirectoryPageQueryDTO;
import com.saibao.invoice.dto.EmployeeDirectoryPageQueryDTO;
import com.saibao.invoice.mapper.DingDirectoryMapper;
import com.saibao.invoice.service.IDingDirectoryService;
import com.saibao.invoice.vo.DingDepartmentVO;
import com.saibao.invoice.vo.DingEmployeeVO;
import com.saibao.invoice.vo.DingOrganizationVO;
import com.saibao.invoice.vo.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** 基于本地钉钉同步目录的分页查询实现。 */
@Service
@RequiredArgsConstructor
public class DingDirectoryServiceImpl implements IDingDirectoryService {
    private final DingDirectoryMapper mapper;

    @Override
    public List<DingOrganizationVO> listOrganizations() {
        return mapper.selectOrganizations();
    }

    @Override
    public PageResult<DingEmployeeVO> pageEmployees(EmployeeDirectoryPageQueryDTO query) {
        long total = mapper.countEmployees(query);
        if (total == 0) {
            return new PageResult<>(Collections.emptyList(), 0, query.getPageNum(), query.getPageSize());
        }
        List<DingEmployee> employees = mapper.selectEmployeePage(query);
        attachDepartmentMemberships(employees);
        var records = employees.stream().map(this::toEmployeeVO).toList();
        return new PageResult<>(records, total, query.getPageNum(), query.getPageSize());
    }

    @Override
    public PageResult<DingDepartmentVO> pageDepartments(DepartmentDirectoryPageQueryDTO query) {
        long total = mapper.countDepartments(query);
        var records = total == 0 ? Collections.<DingDepartmentVO>emptyList()
                : mapper.selectDepartmentPage(query).stream().map(this::toDepartmentVO).toList();
        return new PageResult<>(records, total, query.getPageNum(), query.getPageSize());
    }

    private DingEmployeeVO toEmployeeVO(DingEmployee source) {
        DingEmployeeVO vo = new DingEmployeeVO();
        vo.setId(source.getId());
        vo.setCorpCode(source.getCorpCode());
        vo.setCorpName(source.getCorpName());
        vo.setDingUserId(source.getDingUserId());
        vo.setEmployeeNo(source.getEmployeeNo());
        vo.setEmployeeName(source.getEmployeeName());
        vo.setDepartmentId(source.getDepartmentId());
        vo.setDepartmentIds(source.getDepartmentIds());
        vo.setDepartmentName(source.getDepartmentName());
        vo.setMobile(source.getMobile());
        vo.setStatus(source.getStatus());
        vo.setPermissionEnabled(source.getPermissionEnabled());
        return vo;
    }

    /**
     * 目录表保留主部门快照用于展示；权限编辑需要关联表中的完整多部门归属，必须批量补齐。
     */
    private void attachDepartmentMemberships(List<DingEmployee> employees) {
        if (employees.isEmpty()) {
            return;
        }
        List<Long> employeeIds = employees.stream().map(DingEmployee::getId).toList();
        Map<Long, LinkedHashSet<Long>> departmentIdsByEmployee = new LinkedHashMap<>();
        mapper.selectEmployeeDepartmentMembershipsByEmployeeIds(employeeIds).forEach(membership ->
                departmentIdsByEmployee
                        .computeIfAbsent(membership.getEmployeeId(), ignored -> new LinkedHashSet<>())
                        .add(membership.getDepartmentId()));
        employees.forEach(employee -> {
            LinkedHashSet<Long> departmentIds = departmentIdsByEmployee
                    .computeIfAbsent(employee.getId(), ignored -> new LinkedHashSet<>());
            if (employee.getDepartmentId() != null) {
                departmentIds.add(employee.getDepartmentId());
            }
            employee.setDepartmentIds(List.copyOf(departmentIds));
        });
    }

    private DingDepartmentVO toDepartmentVO(DingDepartment source) {
        DingDepartmentVO vo = new DingDepartmentVO();
        vo.setId(source.getId());
        vo.setCorpCode(source.getCorpCode());
        vo.setCorpName(source.getCorpName());
        vo.setDingDepartmentId(source.getDingDepartmentId());
        vo.setDepartmentName(source.getDepartmentName());
        vo.setParentDepartmentId(source.getParentDepartmentId());
        vo.setStatus(source.getStatus());
        vo.setEmployeeCount(source.getEmployeeCount());
        return vo;
    }
}
