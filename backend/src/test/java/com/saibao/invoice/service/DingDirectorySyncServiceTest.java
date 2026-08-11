package com.saibao.invoice.service;

import com.saibao.invoice.integration.dingtalk.DingDepartmentSnapshot;
import com.saibao.invoice.integration.dingtalk.DingEmployeeSnapshot;
import com.saibao.invoice.integration.dingtalk.DingTalkClient;
import com.saibao.invoice.integration.dingtalk.DingTalkIdentity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@Import(DingDirectorySyncServiceTest.FakeDingTalkConfiguration.class)
@Sql(scripts = {"classpath:schema-test.sql", "classpath:data-test.sql"}, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class DingDirectorySyncServiceTest {

    @Autowired
    private IDingDirectorySyncService syncService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetDirectory() {
        jdbcTemplate.update("DELETE FROM ding_employee_department");
        jdbcTemplate.update("DELETE FROM ding_directory_sync_log");
        jdbcTemplate.update("DELETE FROM ding_employee");
        jdbcTemplate.update("DELETE FROM ding_department");
    }

    @Test
    void syncShouldUpsertDepartmentsEmployeesRelationsAndDeactivateMissingRecords() {
        jdbcTemplate.update("""
                INSERT INTO ding_department
                (id, ding_department_id, department_name, status, sort_no, created_at, updated_at)
                VALUES (99, 'removed-dept', '已删除部门', 'ENABLED', 99, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        jdbcTemplate.update("""
                INSERT INTO ding_employee
                (id, ding_user_id, employee_no, employee_name, department_id, department_name, status, created_at, updated_at)
                VALUES (99, 'removed-user', 'OLD001', '已离职员工', 99, '已删除部门', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);

        var result = syncService.synchronize("MANUAL", "superadmin");

        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getDepartmentCount()).isEqualTo(2);
        assertThat(result.getEmployeeCount()).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM ding_employee WHERE ding_user_id = 'removed-user'", String.class))
                .isEqualTo("INACTIVE");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT union_id FROM ding_employee WHERE ding_user_id = 'ding-user-1001'", String.class))
                .isEqualTo("union-1001");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ding_employee_department", Integer.class)).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM ding_directory_sync_log ORDER BY id DESC LIMIT 1", String.class))
                .isEqualTo("SUCCESS");
    }

    @TestConfiguration
    static class FakeDingTalkConfiguration {
        @Bean
        @Primary
        DingTalkClient fakeDingTalkClient() {
            return new DingTalkClient() {
                @Override
                public DingTalkIdentity resolveIdentity(String authCode) {
                    return new DingTalkIdentity("ding-user-1001", "union-1001");
                }

                @Override
                public List<DingDepartmentSnapshot> listDepartments() {
                    return List.of(
                            new DingDepartmentSnapshot("dept-10", "技术中心", null, 10),
                            new DingDepartmentSnapshot("dept-11", "研发部", "dept-10", 20));
                }

                @Override
                public List<DingEmployeeSnapshot> listEmployees() {
                    return List.of(
                            new DingEmployeeSnapshot("ding-user-1001", "union-1001", "SB1001", "张三",
                                    List.of("dept-10", "dept-11"), "13800001001", true),
                            new DingEmployeeSnapshot("ding-user-1002", "union-1002", "SB1002", "李四",
                                    List.of("dept-10"), "13800001002", true));
                }
            };
        }
    }
}
