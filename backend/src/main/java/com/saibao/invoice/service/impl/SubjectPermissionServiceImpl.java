package com.saibao.invoice.service.impl;

import com.saibao.invoice.domain.SubjectPermission;
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
import com.saibao.invoice.vo.DingEmployeeVO;
import com.saibao.invoice.vo.EmployeePermissionRuleVO;
import com.saibao.invoice.vo.EmployeeSelectionGroupVO;
import com.saibao.invoice.vo.SubjectPermissionVO;
import com.saibao.invoice.vo.SubjectPermissionProfileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 主体权限服务实现。 */
@Service
@RequiredArgsConstructor
public class SubjectPermissionServiceImpl implements ISubjectPermissionService {
    private static final int MAX_SELECTED_EMPLOYEE_COUNT = 5_000;

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
        if (!"USER".equals(request.getTargetType())) {
            throw new IllegalArgumentException("仅支持员工级主体权限，企业和部门仅用于批量选择员工");
        }
        if (request.getTargetCorpCode() == null || request.getTargetCorpCode().isBlank()) {
            throw new IllegalArgumentException("员工所属企业编码不能为空");
        }
        String corpCode = request.getTargetCorpCode().trim();
        String dingUserId = request.getTargetId().trim();
        DingEmployee employee = directoryMapper.selectEmployeeByIdentity(corpCode, dingUserId);
        if (employee == null || !"ACTIVE".equals(employee.getStatus())) {
            throw new IllegalArgumentException("所选员工不存在、已离职或尚未同步");
        }
        SubjectPermission permission = new SubjectPermission();
        permission.setSubjectId(request.getSubjectId()); permission.setTargetType(request.getTargetType());
        permission.setTargetCorpCode(corpCode);
        permission.setTargetId(employee.getDingUserId()); permission.setTargetName(employee.getEmployeeName());
        permission.setPermissionEffect("ALLOW");
        permission.setIncludeChildDepartments(false);
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
        boolean allEmployeeVisible = Boolean.TRUE.equals(subject.getAllEmployeeVisible());
        List<EmployeePermissionRuleVO> employeeRules = allEmployeeVisible
                ? List.of()
                : mapper.selectProfileEmployeeRules(subjectId);
        List<DingEmployeeVO> selectedEmployees = employeeRules.stream()
                .map(this::toEmployeeVO)
                .toList();
        SubjectPermissionProfileVO profile = new SubjectPermissionProfileVO();
        profile.setSubjectId(subject.getId());
        profile.setSubjectName(subject.getSubjectName());
        profile.setAllEmployeeVisible(allEmployeeVisible);
        profile.setVisibleCount(allEmployeeVisible
                ? mapper.countEffectiveEmployees(subjectId)
                : (long) selectedEmployees.size());
        profile.setSelectedEmployeeIds(selectedEmployees.stream().map(DingEmployeeVO::getId).toList());
        profile.setSelectedEmployees(selectedEmployees);
        profile.setEmployeeGroups(groupEmployees(selectedEmployees));

        // 旧版部门授权与排除字段仅为兼容旧客户端保留；新契约只回显最终员工集合。
        profile.setDepartments(List.of());
        profile.setEmployeeRules(employeeRules);
        profile.setDepartmentExcludedEmployeeIds(List.of());
        profile.setPartiallySelectedDepartmentIds(List.of());
        return profile;
    }

    @Override
    @Transactional
    public SubjectPermissionProfileVO saveProfile(Long subjectId,
                                                  SubjectPermissionProfileSaveDTO request,
                                                  String operatorUserId) {
        requireSubject(subjectId);
        boolean allEmployeeVisible = Boolean.TRUE.equals(request.getAllEmployeeVisible());
        if (allEmployeeVisible
                && request.getSelectedEmployeeIds() != null
                && !request.getSelectedEmployeeIds().isEmpty()) {
            throw new IllegalArgumentException("全员可见时不能同时提交已选员工");
        }

        List<Long> selectedEmployeeIds = allEmployeeVisible
                ? List.of()
                : request.getSelectedEmployeeIds() == null
                ? legacySelectedEmployeeIds(request)
                : distinct(request.getSelectedEmployeeIds());
        if (selectedEmployeeIds.size() > MAX_SELECTED_EMPLOYEE_COUNT) {
            throw new IllegalArgumentException("员工选择不能超过 5000 个");
        }
        List<DingEmployee> selectedEmployees = requireActiveEmployees(selectedEmployeeIds);

        if (subjectMapper.updateAllEmployeeVisible(subjectId,
                allEmployeeVisible, operatorUserId) == 0) {
            throw new IllegalArgumentException("展示主体不存在：" + subjectId);
        }

        mapper.deleteDepartmentEmployeeExclusionsBySubjectId(subjectId);
        mapper.deleteBySubjectId(subjectId);
        selectedEmployees.forEach(employee -> mapper.insert(newPermission(
                subjectId, "USER", employee.getCorpCode(), employee.getDingUserId(), employee.getEmployeeName(),
                "ALLOW", false, operatorUserId)));
        return getProfile(subjectId);
    }

    @Override
    @Transactional
    public SubjectPermissionProfileVO updateAllEmployeeVisible(Long subjectId,
                                                               boolean allEmployeeVisible,
                                                               String operatorUserId) {
        InvoiceSubject subject = requireSubject(subjectId);
        if (Boolean.TRUE.equals(subject.getAllEmployeeVisible()) == allEmployeeVisible) {
            return getProfile(subjectId);
        }
        if (subjectMapper.updateAllEmployeeVisible(subjectId, allEmployeeVisible, operatorUserId) == 0) {
            throw new IllegalArgumentException("展示主体不存在：" + subjectId);
        }
        // 两种模式互斥：切换模式时清空旧的部分可见员工集合与历史排除数据。
        mapper.deleteDepartmentEmployeeExclusionsBySubjectId(subjectId);
        mapper.deleteBySubjectId(subjectId);
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
        return ids.stream()
                .filter(id -> id != null && id > 0)
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new), ArrayList::new));
    }

    /**
     * 兼容旧版页面提交：先将企业/部门批量选择展开为在职员工，再应用旧版个人开关，
     * 最终仍只返回员工目录主键集合，后续不会再保存部门或排除规则。
     */
    private List<Long> legacySelectedEmployeeIds(SubjectPermissionProfileSaveDTO request) {
        LinkedHashSet<Long> selected = new LinkedHashSet<>();
        List<Long> selectedDepartmentIds = distinct(request.getDepartmentIds());
        if (!selectedDepartmentIds.isEmpty()) {
            selected.addAll(directoryMapper.selectActiveEmployeeIdsByDepartmentIds(selectedDepartmentIds));
        }
        if (request.getEmployeeRules() != null) {
            request.getEmployeeRules().stream()
                    .filter(rule -> rule != null && "ALLOW".equals(rule.getEffect()))
                    .map(EmployeePermissionRuleDTO::getEmployeeId)
                    .filter(id -> id != null && id > 0)
                    .forEach(selected::add);
        }

        LinkedHashSet<Long> excluded = new LinkedHashSet<>(distinct(request.getDepartmentExcludedEmployeeIds()));
        List<Long> revokedDepartmentIds = new ArrayList<>(distinct(request.getRevokedDepartmentIds()));
        revokedDepartmentIds.removeAll(selectedDepartmentIds);
        if (!revokedDepartmentIds.isEmpty()) {
            excluded.addAll(directoryMapper.selectActiveEmployeeIdsByDepartmentIds(revokedDepartmentIds));
        }
        List<Long> reenabledEmployeeIds = distinct(request.getReenabledEmployeeIds());
        excluded.removeAll(reenabledEmployeeIds);
        selected.removeAll(excluded);
        selected.addAll(reenabledEmployeeIds);
        return new ArrayList<>(selected);
    }

    /** 查询并按请求顺序返回在职员工，防止停用或不存在的目录 ID 被写入权限表。 */
    private List<DingEmployee> requireActiveEmployees(List<Long> employeeIds) {
        if (employeeIds.isEmpty()) return List.of();
        Map<Long, DingEmployee> activeById = directoryMapper.selectEmployeesByIds(employeeIds).stream()
                .collect(Collectors.toMap(DingEmployee::getId, employee -> employee,
                        (first, ignored) -> first, LinkedHashMap::new));
        if (activeById.size() != employeeIds.size() || !activeById.keySet().containsAll(employeeIds)) {
            throw new IllegalArgumentException("所选员工不存在、已离职或尚未同步");
        }
        return employeeIds.stream().map(activeById::get).toList();
    }

    private DingEmployeeVO toEmployeeVO(EmployeePermissionRuleVO rule) {
        DingEmployeeVO employee = new DingEmployeeVO();
        employee.setCorpCode(defaultCorpCode(rule.getCorpCode()));
        employee.setCorpName(rule.getCorpName());
        employee.setId(rule.getEmployeeId());
        employee.setDingUserId(rule.getDingUserId());
        employee.setEmployeeNo(rule.getEmployeeNo());
        employee.setEmployeeName(rule.getEmployeeName());
        employee.setDepartmentIds(List.of());
        employee.setDepartmentName(rule.getDepartmentName());
        employee.setMobile(rule.getMobile());
        employee.setStatus("ACTIVE");
        employee.setPermissionEnabled(true);
        return employee;
    }

    private List<EmployeeSelectionGroupVO> groupEmployees(List<DingEmployeeVO> employees) {
        Map<String, EmployeeSelectionGroupVO> grouped = new LinkedHashMap<>();
        for (DingEmployeeVO employee : employees) {
            String corpCode = defaultCorpCode(employee.getCorpCode());
            EmployeeSelectionGroupVO group = grouped.computeIfAbsent(corpCode, ignored -> {
                EmployeeSelectionGroupVO created = new EmployeeSelectionGroupVO();
                created.setCorpCode(corpCode);
                created.setCorpName(employee.getCorpName());
                return created;
            });
            group.getEmployees().add(employee);
        }
        grouped.values().forEach(group -> group.setEmployeeCount((long) group.getEmployees().size()));
        return new ArrayList<>(grouped.values());
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
