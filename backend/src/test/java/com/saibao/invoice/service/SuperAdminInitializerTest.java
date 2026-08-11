package com.saibao.invoice.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "invoice.auth.super-admin.username=bootstrap.admin",
        "invoice.auth.super-admin.password=Bootstrap@123",
        "invoice.auth.super-admin.display-name=初始超级管理员"
})
class SuperAdminInitializerTest {

    @Autowired
    private IFinanceAccountService financeAccountService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void applicationShouldCreateExactlyOneSuperAdministratorWhenDatabaseIsEmpty() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM finance_user WHERE role_type = 'SUPER_ADMIN' AND deleted = 0", Long.class))
                .isEqualTo(1L);
        assertThat(financeAccountService.authenticate("bootstrap.admin", "Bootstrap@123").getRoleType())
                .isEqualTo("SUPER_ADMIN");
    }
}
