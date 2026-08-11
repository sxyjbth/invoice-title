package com.saibao.invoice.service.impl;

import com.saibao.invoice.domain.FinanceUser;
import com.saibao.invoice.domain.OperationLog;
import com.saibao.invoice.dto.CreateFinanceAccountDTO;
import com.saibao.invoice.dto.FinanceAccountPageQueryDTO;
import com.saibao.invoice.mapper.FinanceUserMapper;
import com.saibao.invoice.mapper.OperationLogMapper;
import com.saibao.invoice.service.IFinanceAccountService;
import com.saibao.invoice.vo.FinanceAccountVO;
import com.saibao.invoice.vo.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Collections;

/** 网页财务端账号服务实现。 */
@Service
@RequiredArgsConstructor
public class FinanceAccountServiceImpl implements IFinanceAccountService {
    private final FinanceUserMapper financeUserMapper;
    private final OperationLogMapper operationLogMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public FinanceAccountVO createFinanceAccount(Long operatorId, CreateFinanceAccountDTO request) {
        FinanceUser operator = requireSuperAdministrator(operatorId);
        String username = request.getUsername().trim().toLowerCase(Locale.ROOT);
        if (financeUserMapper.selectByUsername(username) != null) {
            throw new IllegalArgumentException("登录账号已存在");
        }
        validatePassword(request.getInitialPassword());
        LocalDateTime now = LocalDateTime.now();
        FinanceUser user = new FinanceUser();
        user.setUsername(username);
        user.setDisplayName(request.getDisplayName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getInitialPassword()));
        user.setRoleType("FINANCE");
        user.setStatus("ENABLED");
        user.setPasswordChangedAt(now);
        user.setCreatedBy(operatorId);
        user.setCreatedAt(now);
        user.setUpdatedBy(operatorId);
        user.setUpdatedAt(now);
        financeUserMapper.insert(user);
        writeAccountAudit(operator, user, "CREATE_ACCOUNT", "{\"roleType\":\"FINANCE\",\"status\":\"ENABLED\"}");
        return toVO(user);
    }

    @Override
    public PageResult<FinanceAccountVO> pageFinanceAccounts(FinanceAccountPageQueryDTO query) {
        long total = financeUserMapper.countFinanceAccounts(query);
        var records = total == 0 ? Collections.<FinanceAccountVO>emptyList()
                : financeUserMapper.selectFinanceAccountPage(query).stream().map(this::toVO).toList();
        return new PageResult<>(records, total, query.getPageNum(), query.getPageSize());
    }

    @Override
    @Transactional
    public FinanceAccountVO authenticate(String username, String rawPassword) {
        FinanceUser user = financeUserMapper.selectByUsername(username == null ? "" : username.trim());
        if (user == null || !"ENABLED".equals(user.getStatus()) || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("账号或密码错误");
        }
        LocalDateTime loginAt = LocalDateTime.now();
        financeUserMapper.updateLastLogin(user.getId(), loginAt);
        user.setLastLoginAt(loginAt);
        return toVO(user);
    }

    @Override
    public FinanceAccountVO getById(Long id) {
        return toVO(required(id));
    }

    @Override
    @Transactional
    public void changeOwnPassword(Long accountId, String currentPassword, String newPassword) {
        FinanceUser user = required(accountId);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("当前密码错误");
        }
        updatePassword(user.getId(), user.getId(), newPassword);
        writeAccountAudit(user, user, "CHANGE_PASSWORD", "{\"scope\":\"SELF\"}");
    }

    @Override
    @Transactional
    public void resetPassword(Long operatorId, Long accountId, String newPassword) {
        FinanceUser operator = requireSuperAdministrator(operatorId);
        FinanceUser target = required(accountId);
        if ("SUPER_ADMIN".equals(target.getRoleType())) {
            throw new IllegalArgumentException("超级管理员密码只能由本人修改");
        }
        updatePassword(accountId, operatorId, newPassword);
        writeAccountAudit(operator, target, "RESET_PASSWORD", "{\"reason\":\"ADMIN_RESET\"}");
    }

    @Override
    @Transactional
    public void updateStatus(Long operatorId, Long accountId, String status) {
        FinanceUser operator = requireSuperAdministrator(operatorId);
        if (!"ENABLED".equals(status) && !"DISABLED".equals(status)) {
            throw new IllegalArgumentException("账号状态仅支持 ENABLED 或 DISABLED");
        }
        FinanceUser target = required(accountId);
        if (!"FINANCE".equals(target.getRoleType())) {
            throw new IllegalArgumentException("财务账号不存在");
        }
        if (financeUserMapper.updateStatus(accountId, status, operatorId, LocalDateTime.now()) == 0) {
            throw new IllegalArgumentException("财务账号不存在");
        }
        writeAccountAudit(operator, target,
                "DISABLED".equals(status) ? "DISABLE_ACCOUNT" : "ENABLE_ACCOUNT",
                "{\"status\":\"" + status + "\"}");
    }

    private void updatePassword(Long accountId, Long operatorId, String newPassword) {
        validatePassword(newPassword);
        if (financeUserMapper.updatePassword(accountId, passwordEncoder.encode(newPassword), operatorId, LocalDateTime.now()) == 0) {
            throw new IllegalArgumentException("账号不存在或已停用");
        }
    }

    private FinanceUser requireSuperAdministrator(Long operatorId) {
        FinanceUser operator = required(operatorId);
        if (!"SUPER_ADMIN".equals(operator.getRoleType()) || !"ENABLED".equals(operator.getStatus())) {
            throw new SecurityException("仅超级管理员可以维护财务账号");
        }
        return operator;
    }

    private FinanceUser required(Long id) {
        FinanceUser user = financeUserMapper.selectById(id);
        if (user == null) {
            throw new IllegalArgumentException("账号不存在");
        }
        return user;
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8 || password.length() > 72
                || !password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            throw new IllegalArgumentException("密码须为 8-72 位并同时包含字母和数字");
        }
    }

    /**
     * 记录账号安全操作。日志只写动作与状态，不允许写入任何明文密码或密码摘要。
     */
    private void writeAccountAudit(FinanceUser operator, FinanceUser target, String operationType, String detailJson) {
        OperationLog log = new OperationLog();
        log.setModuleType("ACCOUNT");
        log.setOperationType(operationType);
        log.setBusinessId(String.valueOf(target.getId()));
        log.setBusinessName(target.getDisplayName() + "（" + target.getUsername() + "）");
        log.setDetailJson(detailJson);
        log.setResult("SUCCESS");
        log.setOperatorUserId(String.valueOf(operator.getId()));
        log.setOperatorName(operator.getDisplayName());
        log.setCreatedAt(LocalDateTime.now());
        operationLogMapper.insert(log);
    }

    private FinanceAccountVO toVO(FinanceUser user) {
        FinanceAccountVO vo = new FinanceAccountVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setDisplayName(user.getDisplayName());
        vo.setRoleType(user.getRoleType());
        vo.setStatus(user.getStatus());
        vo.setPasswordChangedAt(user.getPasswordChangedAt());
        vo.setLastLoginAt(user.getLastLoginAt());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }
}
