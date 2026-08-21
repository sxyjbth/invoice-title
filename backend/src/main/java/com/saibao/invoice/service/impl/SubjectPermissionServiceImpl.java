package com.saibao.invoice.service.impl;

import com.saibao.invoice.domain.SubjectPermission;
import com.saibao.invoice.domain.DingDepartment;
import com.saibao.invoice.domain.DingEmployee;
import com.saibao.invoice.domain.InvoiceSubject;
import com.saibao.invoice.dto.EmployeePermissionRuleDTO;
import com.saibao.invoice.dto.SubjectPermissionPageQueryDTO;
import com.saibao.invoice.dto.SubjectPermissionSaveDTO;
import com.saibao.invoice.dto.SubjectPermissionProfileSaveDTO;
import com.saibao.invoice.mapper.DingDirectoryMapper;
import com.saibao.invoice.mapper.InvoiceSubjectMapper;
import com.saibao.invoice.mapper.SubjectPermissionMapper;
import com.saibao.invoice.service.ISubjectPermissionService;
import com.saibao.invoice.vo.PageResult;
import com.saibao.invoice.vo.SubjectPermissionVO;
import com.saibao.invoice.vo.SubjectPermissionProfileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 主体权限服务实现。 */
@Service
@RequiredArgsConstructor
public class SubjectPermissionServiceImpl implements ISubjectPermissionService {
    private final SubjectPermissionMapper mapper;
    private final InvoiceSubjectMapper subjectMapper;
    private final DingDirectoryMapper directoryMapper;

    @Override
    public PageResult<SubjectPermissionVO> page(SubjectPermissionPageQueryDTO query) {
        long total = mapper.count(query);
        List<SubjectPermissionVO> records = total == 0 ? Collections.emptyList() : mapper.selectPage(query).stream().map(this::toVO).toList();
        return new PageResult<>(records, total, query.getPageNum(), query.getPageSize());
    }

    @Override
    public Long create(SubjectPermissionSaveDTO request) {
        SubjectPermission permission = new SubjectPermission();
        permission.setSubjectId(request.getSubjectId()); permission.setTargetType(request.getTargetType());
        permission.setTargetCorpCode(defaultCorpCode(request.getTargetCorpCode()));
        permission.setTargetId(request.getTargetId().trim()); permission.setTargetName(request.getTargetName().trim());
        permission.setPermissionEffect("ALLOW");
        permission.setIncludeChildDepartments("DEPARTMENT".equals(request.getTargetType()));
        permission.setStatus("ENABLED"); permission.setSource("MANUAL"); permission.setCreatedBy(request.getOperatorUserId());
        permission.setUpdatedBy(request.getOperatorUserId()); permission.setCreatedAt(LocalDateTime.now()); permission.setUpdatedAt(LocalDateTime.now());
        mapper.insert(permission);
        return permission.getId();
    }

    @Override
    public void changeStatus(Long id, String status, String operatorUserId) {
        if (mapper.updateStatus(id, status, operatorUserId) == 0) throw new IllegalArgumentException("主体权限不存在：" + id);
    }

    @Override
    public SubjectPermissionProfileVO getProfile(Long subjectId) {
        InvoiceSubject subject = requireSubject(subjectId);
        SubjectPermissionProfileVO profile = new SubjectPermissionProfileVO();
        profile.setSubjectId(subject.getId());
        profile.setSubjectName(subject.getSubjectName());
        profile.setAllEmployeeVisible(Boolean.TRUE.equals(subject.getAllEmployeeVisible()));
        profile.setVisibleCount(mapper.countEffectiveEmployees(subjectId));
        profile.setDepartments(mapper.selectProfileDepartments(subjectId));
        profile.setEmployeeRules(mapper.selectProfileEmployeeRules(subjectId));
        return profile;
    }

    @Override
    @Transactional
    public SubjectPermissionProfileVO saveProfile(Long subjectId,
                                                  SubjectPermissionProfileSaveDTO request,
                                                  String operatorUserId) {
        requireSubject(subjectId);
        List<Long> departmentIds = distinct(request.getDepartmentIds());
        List<Long> employeeIds = request.getEmployeeRules() == null
                ? List.of()
                : request.getEmployeeRules().stream().map(EmployeePermissionRuleDTO::getEmployeeId)
                .collect(Collectors.collectingAndThen(Collectors.toCollection(LinkedHashSet::new), ArrayList::new));

        List<DingDepartment> departments = departmentIds.isEmpty()
                ? List.of() : directoryMapper.selectDepartmentsByIds(departmentIds);
        if (departments.size() != departmentIds.size()) {
            throw new IllegalArgumentException("所选部门不存在、已停用或尚未同步");
        }
        List<DingEmployee> employees = employeeIds.isEmpty()
                ? List.of() : directoryMapper.selectEmployeesByIds(employeeIds);
        if (employees.size() != employeeIds.size()) {
            throw new IllegalArgumentException("所选员工不存在、已离职或尚未同步");
        }

        Map<Long, EmployeePermissionRuleDTO> ruleByEmployeeId = request.getEmployeeRules() == null
                ? Map.of()
                : request.getEmployeeRules().stream().collect(Collectors.toMap(
                        EmployeePermissionRuleDTO::getEmployeeId,
                        Function.identity(),
                        (first, replacement) -> replacement));

        if (subjectMapper.updateAllEmployeeVisible(subjectId,
                Boolean.TRUE.equals(request.getAllEmployeeVisible()), operatorUserId) == 0) {
            throw new IllegalArgumentException("展示主体不存在：" + subjectId);
        }
        mapper.deleteBySubjectId(subjectId);
        departments.forEach(department -> mapper.insert(newPermission(
                subjectId, "DEPARTMENT", department.getCorpCode(), department.getDingDepartmentId(), department.getDepartmentName(),
                "ALLOW", true, operatorUserId)));
        employees.forEach(employee -> mapper.insert(newPermission(
                subjectId, "USER", employee.getCorpCode(), employee.getDingUserId(), employee.getEmployeeName(),
                ruleByEmployeeId.get(employee.getId()).getEffect(), false, operatorUserId)));
        return getProfile(subjectId);
    }

    @Override
    @Transactional
    public SubjectPermissionProfileVO updateAllEmployeeVisible(Long subjectId,
                                                               boolean allEmployeeVisible,
                                                               String operatorUserId) {
        requireSubject(subjectId);
        if (subjectMapper.updateAllEmployeeVisible(subjectId, allEmployeeVisible, operatorUserId) == 0) {
            throw new IllegalArgumentException("展示主体不存在：" + subjectId);
        }
        return getProfile(subjectId);
    }

    private InvoiceSubject requireSubject(Long subjectId) {
        InvoiceSubject subject = subjectMapper.selectById(subjectId);
        if (subject == null) {
            throw new IllegalArgumentException("展示主体不存在：" + subjectId);
        }
        return subject;
    }

    private List<Long> distinct(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return new ArrayList<>(new LinkedHashSet<>(ids));
    }

    private SubjectPermission newPermission(Long subjectId,
                                            String targetType,
                                            String targetCorpCode,
                                            String targetId,
                                            String targetName,
                                            String effect,
                                            boolean includeChildren,
                                            String operatorUserId) {
        SubjectPermission permission = new SubjectPermission();
        permission.setSubjectId(subjectId);
        permission.setTargetType(targetType);
        permission.setTargetCorpCode(defaultCorpCode(targetCorpCode));
        permission.setTargetId(targetId);
        permission.setTargetName(targetName);
        permission.setPermissionEffect(effect);
        permission.setIncludeChildDepartments(includeChildren);
        permission.setStatus("ENABLED");
        permission.setSource("MANUAL");
        permission.setCreatedBy(operatorUserId);
        permission.setUpdatedBy(operatorUserId);
        permission.setCreatedAt(LocalDateTime.now());
        permission.setUpdatedAt(LocalDateTime.now());
        return permission;
    }

    private SubjectPermissionVO toVO(SubjectPermission source) {
        SubjectPermissionVO vo = new SubjectPermissionVO();
        vo.setId(source.getId()); vo.setSubjectId(source.getSubjectId()); vo.setSubjectName(source.getSubjectName());
        vo.setTargetType(source.getTargetType()); vo.setTargetCorpCode(source.getTargetCorpCode());
        vo.setTargetId(source.getTargetId()); vo.setTargetName(source.getTargetName());
        vo.setStatus(source.getStatus()); vo.setSource(source.getSource()); vo.setUpdatedAt(source.getUpdatedAt());
        return vo;
    }

    private String defaultCorpCode(String value) {
        return value == null || value.isBlank() ? "default" : value.trim();
    }
}
