package com.saibao.invoice.service.impl;

import com.saibao.invoice.domain.DingDepartment;
import com.saibao.invoice.domain.DingDirectorySyncLog;
import com.saibao.invoice.domain.DingEmployee;
import com.saibao.invoice.integration.dingtalk.DingDepartmentSnapshot;
import com.saibao.invoice.integration.dingtalk.DingEmployeeSnapshot;
import com.saibao.invoice.integration.dingtalk.DingOrganizationDirectorySnapshot;
import com.saibao.invoice.integration.dingtalk.DingTalkClient;
import com.saibao.invoice.integration.dingtalk.DingTalkRetryExecutor;
import com.saibao.invoice.mapper.DingDirectoryMapper;
import com.saibao.invoice.service.IDingDirectorySyncService;
import com.saibao.invoice.vo.DingDirectorySyncResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 先完整拉取所有企业，再在一个数据库事务中保存；任一企业拉取失败时不会写入半份目录。
 */
@Service
@RequiredArgsConstructor
public class DingDirectorySyncServiceImpl implements IDingDirectorySyncService {
    private final DingTalkClient dingTalkClient;
    private final DingDirectoryMapper mapper;
    private final PlatformTransactionManager transactionManager;
    private final DingTalkRetryExecutor retryExecutor;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @Override
    public DingDirectorySyncResultVO synchronize(String triggerType, String operatorName) {
        validateTrigger(triggerType);
        LocalDateTime startedAt = LocalDateTime.now();
        if (!running.compareAndSet(false, true)) {
            return skipped(triggerType, startedAt);
        }

        DingDirectorySyncLog log = new DingDirectorySyncLog();
        log.setTriggerType(triggerType);
        log.setStatus("RUNNING");
        log.setDepartmentCount(0);
        log.setEmployeeCount(0);
        log.setOperatorName(isBlank(operatorName) ? "system" : operatorName);
        log.setStartedAt(startedAt);
        try {
            mapper.insertSyncLog(log);
            List<DingOrganizationDirectorySnapshot> directories = retryExecutor.executeSync(
                    dingTalkClient::listDirectories);
            validateOrganizations(directories);

            TransactionTemplate transaction = new TransactionTemplate(transactionManager);
            transaction.executeWithoutResult(status -> persistSnapshots(directories));

            log.setStatus("SUCCESS");
            log.setDepartmentCount(directories.stream().mapToInt(item -> item.departments().size()).sum());
            log.setEmployeeCount(directories.stream().mapToInt(item -> item.employees().size()).sum());
            log.setFinishedAt(LocalDateTime.now());
            mapper.finishSyncLog(log);
            return toResult(log, null);
        } catch (RuntimeException exception) {
            String message = safeMessage(exception);
            log.setStatus("FAILED");
            log.setErrorMessage(message);
            log.setFinishedAt(LocalDateTime.now());
            if (log.getId() != null) {
                mapper.finishSyncLog(log);
            }
            throw new IllegalStateException("钉钉通讯录同步失败：" + message, exception);
        } finally {
            running.set(false);
        }
    }

    private void validateOrganizations(List<DingOrganizationDirectorySnapshot> directories) {
        if (directories == null || directories.isEmpty()) {
            throw new IllegalStateException("钉钉企业配置列表为空");
        }
        Set<String> corpCodes = new HashSet<>();
        for (DingOrganizationDirectorySnapshot directory : directories) {
            requireText(directory.corpCode(), "企业业务编码");
            if (!corpCodes.add(directory.corpCode())) {
                throw new IllegalStateException("钉钉企业业务编码重复：" + directory.corpCode());
            }
        }
    }

    private void persistSnapshots(List<DingOrganizationDirectorySnapshot> directories) {
        for (DingOrganizationDirectorySnapshot directory : directories) {
            persistOrganization(directory);
        }
    }

    private void persistOrganization(DingOrganizationDirectorySnapshot directory) {
        LocalDateTime syncedAt = LocalDateTime.now();
        Map<String, DingDepartment> savedDepartments = new HashMap<>();
        Set<String> departmentIds = new HashSet<>();
        for (DingDepartmentSnapshot source : directory.departments()) {
            requireText(source.dingDepartmentId(), "部门 ID");
            requireText(source.departmentName(), "部门名称");
            if (!departmentIds.add(source.dingDepartmentId())) {
                throw new IllegalStateException(directory.corpCode() + " 返回了重复部门 ID：" + source.dingDepartmentId());
            }
            DingDepartment target = mapper.selectDepartmentByDingId(directory.corpCode(), source.dingDepartmentId());
            boolean insert = target == null;
            if (insert) {
                target = new DingDepartment();
            }
            target.setCorpCode(directory.corpCode());
            target.setCorpName(defaultText(directory.corpName(), directory.corpCode()));
            target.setCorpId(directory.corpId());
            target.setDingDepartmentId(source.dingDepartmentId());
            target.setDepartmentName(source.departmentName());
            target.setParentDepartmentId(source.parentDepartmentId());
            target.setStatus("ENABLED");
            target.setSortNo(source.sortNo());
            target.setLastSyncedAt(syncedAt);
            if (insert) mapper.insertDepartment(target); else mapper.updateDepartment(target);
            savedDepartments.put(target.getDingDepartmentId(), target);
        }
        mapper.disableDepartmentsNotIn(directory.corpCode(), List.copyOf(departmentIds));

        Set<String> employeeIds = new HashSet<>();
        for (DingEmployeeSnapshot source : directory.employees()) {
            requireText(source.dingUserId(), "员工 userId");
            requireText(source.employeeName(), "员工姓名");
            if (!employeeIds.add(source.dingUserId())) {
                throw new IllegalStateException(directory.corpCode() + " 返回了重复员工 userId：" + source.dingUserId());
            }
            List<DingDepartment> departments = source.departmentIds() == null ? List.of()
                    : source.departmentIds().stream().map(savedDepartments::get).filter(java.util.Objects::nonNull).toList();
            if (departments.isEmpty()) {
                throw new IllegalStateException("员工 " + directory.corpCode() + "/" + source.dingUserId() + " 没有有效所属部门");
            }
            DingDepartment primary = departments.getFirst();
            DingEmployee target = mapper.selectAnyEmployeeByIdentity(directory.corpCode(), source.dingUserId());
            boolean insert = target == null;
            if (insert) {
                target = new DingEmployee();
            }
            target.setCorpCode(directory.corpCode());
            target.setCorpName(defaultText(directory.corpName(), directory.corpCode()));
            target.setCorpId(directory.corpId());
            target.setDingUserId(source.dingUserId());
            target.setUnionId(source.unionId());
            target.setEmployeeNo(defaultText(source.employeeNo(), source.dingUserId()));
            target.setEmployeeName(source.employeeName());
            target.setDepartmentId(primary.getId());
            target.setDepartmentName(primary.getDepartmentName());
            target.setMobile(source.mobile());
            target.setStatus(source.active() ? "ACTIVE" : "INACTIVE");
            target.setLastSyncedAt(syncedAt);
            if (insert) mapper.insertEmployee(target); else mapper.updateEmployee(target);

            mapper.deleteEmployeeDepartments(target.getId());
            for (int index = 0; index < departments.size(); index++) {
                mapper.insertEmployeeDepartment(target.getId(), departments.get(index).getId(), index == 0);
            }
        }
        mapper.inactivateEmployeesNotIn(directory.corpCode(), List.copyOf(employeeIds));
    }

    private DingDirectorySyncResultVO skipped(String triggerType, LocalDateTime startedAt) {
        DingDirectorySyncResultVO result = new DingDirectorySyncResultVO();
        result.setTriggerType(triggerType);
        result.setStatus("SKIPPED");
        result.setMessage("已有通讯录同步任务正在执行，本次请求已跳过");
        result.setStartedAt(startedAt);
        result.setFinishedAt(LocalDateTime.now());
        return result;
    }

    private DingDirectorySyncResultVO toResult(DingDirectorySyncLog log, String message) {
        DingDirectorySyncResultVO result = new DingDirectorySyncResultVO();
        result.setSyncLogId(log.getId());
        result.setTriggerType(log.getTriggerType());
        result.setStatus(log.getStatus());
        result.setDepartmentCount(log.getDepartmentCount());
        result.setEmployeeCount(log.getEmployeeCount());
        result.setMessage(message);
        result.setStartedAt(log.getStartedAt());
        result.setFinishedAt(log.getFinishedAt());
        return result;
    }

    private void validateTrigger(String triggerType) {
        if (!"MANUAL".equals(triggerType) && !"SCHEDULED".equals(triggerType)) {
            throw new IllegalArgumentException("同步触发类型仅支持 MANUAL 或 SCHEDULED");
        }
    }

    private void requireText(String value, String field) {
        if (isBlank(value)) throw new IllegalStateException("钉钉通讯录数据缺少" + field);
    }

    private String defaultText(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        if (isBlank(message)) message = "未知错误";
        return message.length() > 900 ? message.substring(0, 900) : message;
    }
}
