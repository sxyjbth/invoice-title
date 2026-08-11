package com.saibao.invoice.service;

import com.saibao.invoice.integration.dingtalk.DingDepartmentSnapshot;
import com.saibao.invoice.integration.dingtalk.DingEmployeeSnapshot;
import com.saibao.invoice.integration.dingtalk.DingOrganizationDirectorySnapshot;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@Import(MultiOrganizationDirectorySyncTest.FakeConfiguration.class)
class MultiOrganizationDirectorySyncTest {

    @Autowired private IDingDirectorySyncService syncService;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearDirectory() {
        jdbcTemplate.update("DELETE FROM ding_employee_department");
        jdbcTemplate.update("DELETE FROM ding_directory_sync_log");
        jdbcTemplate.update("DELETE FROM ding_employee");
        jdbcTemplate.update("DELETE FROM ding_department");
    }

    @Test
    void sameDingUserIdInTwoOrganizationsShouldPersistAsTwoEmployeeIdentities() {
        var result = syncService.synchronize("MANUAL", "superadmin");

        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getEmployeeCount()).isEqualTo(2);
        assertThat(jdbcTemplate.queryForList(
                "SELECT corp_code FROM ding_employee WHERE ding_user_id = 'same-user' ORDER BY corp_code",
                String.class)).containsExactly("sebo", "walden");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ding_department WHERE ding_department_id = '1'", Integer.class)).isEqualTo(2);
    }

    @TestConfiguration
    static class FakeConfiguration {
        @Bean
        @Primary
        DingTalkClient multiOrganizationDingTalkClient() {
            return new DingTalkClient() {
                @Override
                public DingTalkIdentity resolveIdentity(String authCode) {
                    return new DingTalkIdentity("same-user", "union-user");
                }

                @Override
                public List<DingDepartmentSnapshot> listDepartments() {
                    return List.of();
                }

                @Override
                public List<DingEmployeeSnapshot> listEmployees() {
                    return List.of();
                }

                @Override
                public List<DingOrganizationDirectorySnapshot> listDirectories() {
                    return List.of(directory("sebo", "赛宝绿创能源技术（上海）有限公司", "ding-sebo", "S001"),
                            directory("walden", "瓦尔登环境科学研究院（北京）有限公司", "ding-walden", "W001"));
                }
            };
        }

        private DingOrganizationDirectorySnapshot directory(
                String corpCode, String corpName, String corpId, String employeeNo) {
            return new DingOrganizationDirectorySnapshot(
                    corpCode,
                    corpName,
                    corpId,
                    List.of(new DingDepartmentSnapshot("1", "企业根部门", null, 0)),
                    List.of(new DingEmployeeSnapshot("same-user", "union-user", employeeNo, "同名员工",
                            List.of("1"), "13800000000", true)));
        }
    }
}
