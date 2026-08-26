package com.saibao.invoice.service.impl;

import com.saibao.invoice.domain.DingDepartment;
import com.saibao.invoice.domain.DingEmployee;
import com.saibao.invoice.dto.DepartmentDirectoryPageQueryDTO;
import com.saibao.invoice.dto.EmployeeDirectoryPageQueryDTO;
import com.saibao.invoice.dto.EmployeeSelectionResolveDTO;
import com.saibao.invoice.mapper.DingDirectoryMapper;
import com.saibao.invoice.service.IDingDirectoryService;
import com.saibao.invoice.vo.DingDepartmentVO;
import com.saibao.invoice.vo.DingEmployeeVO;
import com.saibao.invoice.vo.DingOrganizationVO;
import com.saibao.invoice.vo.EmployeeSelectionGroupVO;
import com.saibao.invoice.vo.EmployeeSelectionResolveVO;
import com.saibao.invoice.vo.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** 基于本地钉钉同步目录的分页查询实现。 */
@Service
@RequiredArgsConstructor
public class DingDirectoryServiceImpl implements IDingDirectoryService {
    /** 单次权限保存允许的最终员工上限，与保存 DTO 的限制保持一致。 */
    private static final int MAX_SELECTED_EMPLOYEE_COUNT = 5_000;

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
     * 把企业、部门和员工三类目录选择展开为最终员工集合。所有入口只保留在职员工，
     * 并以员工目录主键去重，避免员工同时被企业、部门或个人选择时重复展示。
     */
    @Override
    public EmployeeSelectionResolveVO resolveEmployeeSelections(EmployeeSelectionResolveDTO request) {
        EmployeeSelectionResolveVO response = new EmployeeSelectionResolveVO();
        if (request == null) {
            return response;
        }

        Map<Long, DingEmployee> employeesById = new LinkedHashMap<>();
        normalizedCorpCodes(request.getCorpCodes()).forEach(corpCode ->
                appendOrganizationEmployees(corpCode, employeesById));

        LinkedHashSet<Long> employeeIds = normalizedIds(request.getEmployeeIds());
        List<Long> departmentIds = List.copyOf(normalizedIds(request.getDepartmentIds()));
        if (!departmentIds.isEmpty()) {
            employeeIds.addAll(mapper.selectActiveEmployeeIdsByDepartmentIds(departmentIds));
        }
        if (!employeeIds.isEmpty()) {
            mapper.selectEmployeesByIds(List.copyOf(employeeIds)).forEach(employee ->
                    employeesById.putIfAbsent(employee.getId(), employee));
        }

        if (employeesById.size() > MAX_SELECTED_EMPLOYEE_COUNT) {
            throw new IllegalArgumentException("单次最多选择 5000 名员工");
        }

        List<DingEmployee> employees = new ArrayList<>(employeesById.values());
        attachDepartmentMemberships(employees);
        List<DingEmployeeVO> selectedEmployees = employees.stream().map(this::toEmployeeVO).toList();
        response.setSelectedEmployeeCount((long) selectedEmployees.size());
        response.setSelectedEmployeeIds(selectedEmployees.stream().map(DingEmployeeVO::getId).toList());
        response.setSelectedEmployees(selectedEmployees);
        response.setEmployeeGroups(groupEmployees(selectedEmployees));
        return response;
    }

    private void appendOrganizationEmployees(String corpCode, Map<Long, DingEmployee> employeesById) {
        EmployeeDirectoryPageQueryDTO query = new EmployeeDirectoryPageQueryDTO();
        query.setCorpCode(corpCode);
        query.setPageSize(100);
        long total = mapper.countEmployees(query);
        int pageCount = (int) ((total + query.getPageSize() - 1) / query.getPageSize());
        for (int pageNum = 1; pageNum <= pageCount; pageNum++) {
            query.setPageNum(pageNum);
            List<DingEmployee> employees = mapper.selectEmployeePage(query);
            employees.forEach(employee -> employeesById.putIfAbsent(employee.getId(), employee));
            if (employees.isEmpty()) {
                break;
            }
        }
    }

    private List<EmployeeSelectionGroupVO> groupEmployees(List<DingEmployeeVO> employees) {
        Map<String, EmployeeSelectionGroupVO> groups = new LinkedHashMap<>();
        employees.forEach(employee -> {
            String corpCode = employee.getCorpCode() == null ? "" : employee.getCorpCode();
            EmployeeSelectionGroupVO group = groups.computeIfAbsent(corpCode, ignored -> {
                EmployeeSelectionGroupVO value = new EmployeeSelectionGroupVO();
                value.setCorpCode(employee.getCorpCode());
                value.setCorpName(employee.getCorpName());
                return value;
            });
            group.getEmployees().add(employee);
        });
        groups.values().forEach(group -> group.setEmployeeCount((long) group.getEmployees().size()));
        return new ArrayList<>(groups.values());
    }

    private List<String> normalizedCorpCodes(List<String> corpCodes) {
        if (corpCodes == null) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        corpCodes.forEach(corpCode -> {
            if (corpCode != null && !corpCode.isBlank()) {
                normalized.add(corpCode.trim());
            }
        });
        return List.copyOf(normalized);
    }

    private LinkedHashSet<Long> normalizedIds(List<Long> ids) {
        LinkedHashSet<Long> normalized = new LinkedHashSet<>();
        if (ids != null) {
            ids.stream().filter(id -> id != null && id > 0).forEach(normalized::add);
        }
        return normalized;
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
