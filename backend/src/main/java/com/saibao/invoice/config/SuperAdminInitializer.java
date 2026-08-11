package com.saibao.invoice.config;

import com.saibao.invoice.domain.FinanceUser;
import com.saibao.invoice.mapper.FinanceUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * 首次启动时创建唯一的超级管理员账号。
 *
 * <p>数据库内已经存在超级管理员时绝不覆盖其密码，避免重启导致账号失效。</p>
 */
@Component
@RequiredArgsConstructor
public class SuperAdminInitializer implements ApplicationRunner {
    private final FinanceUserMapper financeUserMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${invoice.auth.super-admin.username:superadmin}")
    private String username;

    @Value("${invoice.auth.super-admin.password:ChangeMe@123}")
    private String password;

    @Value("${invoice.auth.super-admin.display-name:超级管理员}")
    private String displayName;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        long administratorCount = financeUserMapper.countSuperAdministrators();
        if (administratorCount > 1) {
            throw new IllegalStateException("数据库中存在多个超级管理员，请先修正账号数据");
        }
        if (administratorCount == 1) {
            return;
        }
        validateBootstrapAccount();

        LocalDateTime now = LocalDateTime.now();
        FinanceUser administrator = new FinanceUser();
        administrator.setUsername(username.trim().toLowerCase(Locale.ROOT));
        administrator.setDisplayName(displayName.trim());
        administrator.setPasswordHash(passwordEncoder.encode(password));
        administrator.setRoleType("SUPER_ADMIN");
        administrator.setStatus("ENABLED");
        administrator.setPasswordChangedAt(now);
        administrator.setCreatedBy(0L);
        administrator.setCreatedAt(now);
        administrator.setUpdatedBy(0L);
        administrator.setUpdatedAt(now);
        financeUserMapper.insert(administrator);
    }

    private void validateBootstrapAccount() {
        if (username == null || username.isBlank() || displayName == null || displayName.isBlank()) {
            throw new IllegalStateException("超级管理员账号和显示名称不能为空");
        }
        if (financeUserMapper.selectByUsername(username.trim()) != null) {
            throw new IllegalStateException("超级管理员登录账号已被其他角色占用");
        }
        if (password == null || password.length() < 8 || password.length() > 72
                || !password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            throw new IllegalStateException("超级管理员初始密码须为 8-72 位并同时包含字母和数字");
        }
    }
}
