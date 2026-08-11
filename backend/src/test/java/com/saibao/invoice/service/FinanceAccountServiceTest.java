package com.saibao.invoice.service;

import com.saibao.invoice.dto.CreateFinanceAccountDTO;
import com.saibao.invoice.dto.FinanceAccountPageQueryDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class FinanceAccountServiceTest {

    @Autowired
    private IFinanceAccountService financeAccountService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void seedSuperAdministrator() {
        jdbcTemplate.update("DELETE FROM invoice_operation_log");
        jdbcTemplate.update("DELETE FROM finance_user");
        jdbcTemplate.update("""
                INSERT INTO finance_user
                (id, username, display_name, password_hash, role_type, status, created_by, created_at, updated_by, updated_at, deleted)
                VALUES (1, 'superadmin', '超级管理员', ?, 'SUPER_ADMIN', 'ENABLED', 1, CURRENT_TIMESTAMP, 1, CURRENT_TIMESTAMP, 0)
                """, new BCryptPasswordEncoder().encode("Admin@123456"));
    }

    @Test
    void superAdministratorShouldCreateFinanceAccountAndPasswordMustBeHashed() {
        CreateFinanceAccountDTO request = new CreateFinanceAccountDTO();
        request.setUsername("finance.wang");
        request.setDisplayName("王财务");
        request.setInitialPassword("Finance@123");

        var created = financeAccountService.createFinanceAccount(1L, request);

        assertThat(created.getRoleType()).isEqualTo("FINANCE");
        assertThat(financeAccountService.authenticate("finance.wang", "Finance@123").getId())
                .isEqualTo(created.getId());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT password_hash FROM finance_user WHERE id = ?", String.class, created.getId()))
                .startsWith("$2").doesNotContain("Finance@123");
    }

    @Test
    void financeShouldChangeOwnPasswordAndAdministratorShouldBeAbleToResetIt() {
        CreateFinanceAccountDTO request = new CreateFinanceAccountDTO();
        request.setUsername("finance.li");
        request.setDisplayName("李会计");
        request.setInitialPassword("Finance@123");
        var finance = financeAccountService.createFinanceAccount(1L, request);

        financeAccountService.changeOwnPassword(finance.getId(), "Finance@123", "Changed@456");
        assertThatThrownBy(() -> financeAccountService.authenticate("finance.li", "Finance@123"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(financeAccountService.authenticate("finance.li", "Changed@456").getId()).isEqualTo(finance.getId());

        financeAccountService.resetPassword(1L, finance.getId(), "Reset@789");
        assertThatThrownBy(() -> financeAccountService.authenticate("finance.li", "Changed@456"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(financeAccountService.authenticate("finance.li", "Reset@789").getId()).isEqualTo(finance.getId());
    }

    @Test
    void financeAccountMustNotManageOtherFinanceAccounts() {
        CreateFinanceAccountDTO first = new CreateFinanceAccountDTO();
        first.setUsername("finance.operator");
        first.setDisplayName("普通财务");
        first.setInitialPassword("Finance@123");
        var finance = financeAccountService.createFinanceAccount(1L, first);

        CreateFinanceAccountDTO second = new CreateFinanceAccountDTO();
        second.setUsername("finance.forbidden");
        second.setDisplayName("不可创建");
        second.setInitialPassword("Finance@123");

        assertThatThrownBy(() -> financeAccountService.createFinanceAccount(finance.getId(), second))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("超级管理员");
    }

    @Test
    void financeAccountListShouldBePaginatedAndDisabledAccountMustNotLogin() {
        createFinance("finance.one", "财务一");
        var second = createFinance("finance.two", "财务二");

        FinanceAccountPageQueryDTO query = new FinanceAccountPageQueryDTO();
        query.setPageNum(1);
        query.setPageSize(1);
        assertThat(financeAccountService.pageFinanceAccounts(query).getTotal()).isEqualTo(2);
        assertThat(financeAccountService.pageFinanceAccounts(query).getRecords()).hasSize(1);

        financeAccountService.updateStatus(1L, second.getId(), "DISABLED");
        assertThatThrownBy(() -> financeAccountService.authenticate("finance.two", "Finance@123"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void accountAndPasswordChangesShouldBeAuditedWithoutRecordingPasswords() {
        var finance = createFinance("finance.audit", "审计财务");
        financeAccountService.changeOwnPassword(finance.getId(), "Finance@123", "Changed@456");
        financeAccountService.resetPassword(1L, finance.getId(), "Reset@789");
        financeAccountService.updateStatus(1L, finance.getId(), "DISABLED");

        var operationTypes = jdbcTemplate.queryForList(
                "SELECT operation_type FROM invoice_operation_log WHERE module_type = 'ACCOUNT' ORDER BY id",
                String.class);
        assertThat(operationTypes).containsExactly(
                "CREATE_ACCOUNT", "CHANGE_PASSWORD", "RESET_PASSWORD", "DISABLE_ACCOUNT");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COALESCE(GROUP_CONCAT(detail_json), '') FROM invoice_operation_log WHERE module_type = 'ACCOUNT'",
                String.class))
                .doesNotContain("Finance@123", "Changed@456", "Reset@789");
    }

    private com.saibao.invoice.vo.FinanceAccountVO createFinance(String username, String name) {
        CreateFinanceAccountDTO request = new CreateFinanceAccountDTO();
        request.setUsername(username);
        request.setDisplayName(name);
        request.setInitialPassword("Finance@123");
        return financeAccountService.createFinanceAccount(1L, request);
    }
}
