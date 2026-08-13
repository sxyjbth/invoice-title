package com.saibao.invoice.service;

import com.saibao.invoice.dto.EmployeeInvoiceTitlePageQueryDTO;
import com.saibao.invoice.vo.InvoiceTitleVO;
import com.saibao.invoice.vo.PageResult;
import com.saibao.invoice.vo.QrTokenVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.apache.ibatis.session.SqlSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class EmployeeAccessAndQrServiceTest {

    @Autowired private IEmployeeInvoiceTitleService employeeInvoiceTitleService;
    @Autowired private IQrTokenService qrTokenService;
    @Autowired private IInvoiceTitleService invoiceTitleService;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private SqlSession sqlSession;

    @Test
    void employeeShouldOnlySeePublishedTitlesOfAuthorizedSubjects() {
        EmployeeInvoiceTitlePageQueryDTO authorizedQuery = new EmployeeInvoiceTitlePageQueryDTO();
        PageResult<InvoiceTitleVO> authorized = employeeInvoiceTitleService.pageAuthorized(authorizedQuery, 1L);
        assertThat(authorized.getTotal()).isEqualTo(1);
        assertThat(authorized.getRecords()).extracting(InvoiceTitleVO::getCompanyName)
                .containsExactly("杭州赛宝卓越技术有限公司");

        EmployeeInvoiceTitlePageQueryDTO unauthorizedQuery = new EmployeeInvoiceTitlePageQueryDTO();
        assertThat(employeeInvoiceTitleService.pageAuthorized(unauthorizedQuery, 999L).getTotal()).isZero();
    }

    @Test
    void employeeTitleShouldExposePublisherDisplayNameAndActualUpdateTime() {
        jdbcTemplate.update("""
                INSERT INTO finance_user
                (username, display_name, password_hash, role_type, status, created_by, created_at, updated_by, updated_at, deleted)
                VALUES ('publisher-user', '王财务', 'not-used', 'FINANCE', 'ENABLED', 0,
                        '2026-08-01 09:00:00', 0, '2026-08-01 09:00:00', 0)
                """);
        jdbcTemplate.update("""
                UPDATE invoice_title
                SET updated_by = 'publisher-user', updated_at = '2026-08-12 16:30:45'
                WHERE id = 1
                """);
        sqlSession.clearCache();

        InvoiceTitleVO title = employeeInvoiceTitleService
                .pageAuthorized(new EmployeeInvoiceTitlePageQueryDTO(), 1L)
                .getRecords().get(0);

        assertThat(title.getUpdatedBy()).isEqualTo("王财务");
        assertThat(title.getUpdatedAt()).isEqualTo(LocalDateTime.of(2026, 8, 12, 16, 30, 45));
    }

    @Test
    void qrTokenShouldExpireInTenMinutesAndBecomeInvalidImmediatelyAfterDisable() {
        LocalDateTime beforeCreate = LocalDateTime.now();
        QrTokenVO token = qrTokenService.create(1L, 1L);

        assertThat(token.getToken()).hasSizeGreaterThanOrEqualTo(24);
        assertThat(Duration.between(beforeCreate, token.getExpiresAt()).toSeconds()).isBetween(599L, 601L);
        assertThat(qrTokenService.resolve(token.getToken()).getTaxpayerId()).isEqualTo("91110400MADFF1HE1T");

        invoiceTitleService.disable(1L, "ding-user-finance");
        assertThatThrownBy(() -> qrTokenService.resolve(token.getToken()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("失效");
    }

    @Test
    void qrSnapshotShouldExposePublisherDisplayNameAndPublishedVersionTime() {
        jdbcTemplate.update("""
                INSERT INTO finance_user
                (username, display_name, password_hash, role_type, status, created_by, created_at, updated_by, updated_at, deleted)
                VALUES ('qr-publisher', '李会计', 'not-used', 'FINANCE', 'ENABLED', 0,
                        '2026-08-01 09:00:00', 0, '2026-08-01 09:00:00', 0)
                """);
        jdbcTemplate.update("""
                UPDATE invoice_title_version
                SET created_by = 'qr-publisher', created_at = '2026-08-12 17:05:30'
                WHERE id = 3
                """);
        sqlSession.clearCache();

        QrTokenVO token = qrTokenService.create(1L, 1L);
        var snapshot = qrTokenService.resolve(token.getToken());

        assertThat(snapshot.getCreatedBy()).isEqualTo("李会计");
        assertThat(snapshot.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 8, 12, 17, 5, 30));
    }

    @Test
    void explicitEmployeeRuleShouldOverrideDepartmentRuleAndAllowIndividualExclusion() {
        jdbcTemplate.update("DELETE FROM subject_permission WHERE subject_id = 1");
        jdbcTemplate.update("""
                INSERT INTO subject_permission
                (subject_id, target_type, target_corp_code, target_id, target_name, permission_effect,
                 status, source, created_by, updated_by, deleted)
                VALUES
                (1, 'DEPARTMENT', 'default', 'ding-dept-tech', '技术中心', 'ALLOW', 'ENABLED', 'MANUAL', 'admin', 'admin', 0),
                (1, 'USER', 'default', 'ding-employee-001', '示例员工', 'DENY', 'ENABLED', 'MANUAL', 'admin', 'admin', 0),
                (1, 'USER', 'default', 'ding-employee-003', '采购员工', 'ALLOW', 'ENABLED', 'MANUAL', 'admin', 'admin', 0)
                """);

        assertThat(totalFor(1L)).as("部门已授权但员工明确拒绝").isZero();
        assertThat(totalFor(4L)).as("同部门且无员工覆盖规则").isEqualTo(1);
        assertThat(totalFor(3L)).as("部门未授权但员工明确允许").isEqualTo(1);
        assertThatThrownBy(() -> qrTokenService.create(1L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("没有该抬头的查看权限");
    }

    @Test
    void permissionForSameDingUserIdMustNotLeakAcrossOrganizations() {
        jdbcTemplate.update("""
                INSERT INTO ding_department
                (id, corp_code, corp_name, ding_department_id, department_name, status, sort_no)
                VALUES (10, 'walden', '瓦尔登环境科学研究院（北京）有限公司',
                        'ding-dept-tech', '技术中心', 'ENABLED', 10)
                """);
        jdbcTemplate.update("""
                INSERT INTO ding_employee
                (id, corp_code, corp_name, ding_user_id, employee_no, employee_name,
                 department_id, department_name, mobile, status)
                VALUES (10, 'walden', '瓦尔登环境科学研究院（北京）有限公司',
                        'ding-employee-001', 'WD0001', '瓦尔登同ID员工', 10, '技术中心', '13900000001', 'ACTIVE')
                """);
        jdbcTemplate.update("""
                INSERT INTO ding_employee_department (employee_id, department_id, is_primary)
                VALUES (10, 10, 1)
                """);

        assertThat(totalFor(1L)).isEqualTo(1);
        assertThat(totalFor(10L)).as("default 企业的个人授权不能泄漏到 walden").isZero();

        jdbcTemplate.update("""
                INSERT INTO subject_permission
                (subject_id, target_type, target_corp_code, target_id, target_name,
                 permission_effect, status, source, created_by, updated_by, deleted)
                VALUES (1, 'USER', 'walden', 'ding-employee-001', '瓦尔登同ID员工',
                        'ALLOW', 'ENABLED', 'MANUAL', 'admin', 'admin', 0)
                """);
        sqlSession.clearCache();
        assertThat(totalFor(10L)).isEqualTo(1);
    }

    private long totalFor(Long employeeId) {
        EmployeeInvoiceTitlePageQueryDTO query = new EmployeeInvoiceTitlePageQueryDTO();
        return employeeInvoiceTitleService.pageAuthorized(query, employeeId).getTotal();
    }
}
