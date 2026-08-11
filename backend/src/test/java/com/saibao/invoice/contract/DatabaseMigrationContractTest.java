package com.saibao.invoice.contract;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseMigrationContractTest {

    private static final Path MIGRATION = Path.of(
            "src", "main", "resources", "db", "migration", "V1__create_invoice_title_schema.sql");

    @Test
    void backendDeclaresSpringBootFlywayStarterSoMigrationsRunOnApplicationStartup() throws IOException {
        String pom = Files.readString(Path.of("pom.xml"), StandardCharsets.UTF_8);

        assertThat(pom).contains("<artifactId>spring-boot-starter-flyway</artifactId>");
    }

    @Test
    void migrationDefinesAllBusinessTablesAndPermissionSwitchFields() throws IOException {
        String ddl = Files.readString(MIGRATION, StandardCharsets.UTF_8);
        List<String> requiredTables = List.of(
                "finance_user",
                "invoice_subject",
                "invoice_title",
                "invoice_title_subject",
                "subject_permission",
                "invoice_title_version",
                "invoice_import_task",
                "invoice_import_row_error",
                "invoice_qr_token",
                "invoice_operation_log");

        requiredTables.forEach(table -> assertThat(ddl).contains("CREATE TABLE " + table));

        Matcher tableMatcher = Pattern.compile("CREATE TABLE\\s+[a-z_]+\\s*\\(", Pattern.CASE_INSENSITIVE)
                .matcher(ddl);
        int tableCount = 0;
        while (tableMatcher.find()) {
            tableCount++;
        }
        assertThat(tableCount).isEqualTo(requiredTables.size());

        assertThat(ddl)
                .contains("all_employee_visible TINYINT UNSIGNED NOT NULL DEFAULT 0")
                .contains("全员可见开关：0-关闭，仅按部门或员工授权；1-开启，全部在职员工可见")
                .contains("include_child_departments TINYINT UNSIGNED NOT NULL DEFAULT 1")
                .contains("是否包含子部门：0-不包含，1-包含；仅部门授权时有效");
    }

    @Test
    void directoryMigrationDefinesEmployeesDepartmentsAndExplicitPermissionPriority() throws IOException {
        Path migration = Path.of(
                "src", "main", "resources", "db", "migration",
                "V101__add_ding_directory_and_permission_effect.sql");

        assertThat(migration).exists();
        String ddl = Files.readString(migration, StandardCharsets.UTF_8);
        assertThat(ddl)
                .contains("CREATE TABLE ding_department")
                .contains("CREATE TABLE ding_employee")
                .contains("employee_no VARCHAR(50) NOT NULL COMMENT '员工工号'")
                .contains("mobile VARCHAR(30) DEFAULT NULL COMMENT '员工手机号")
                .contains("permission_effect VARCHAR(20) NOT NULL DEFAULT 'ALLOW'")
                .contains("权限效果：ALLOW-允许查看，DENY-禁止查看；员工级规则优先于部门级规则");
    }

    @Test
    void dingTalkSyncMigrationDefinesLoginFieldsMultiDepartmentRelationAndAuditableSyncLog() throws IOException {
        Path migration = Path.of(
                "src", "main", "resources", "db", "migration",
                "V103__add_dingtalk_sync_and_employee_login.sql");

        assertThat(migration).exists();
        String ddl = Files.readString(migration, StandardCharsets.UTF_8);
        assertThat(ddl)
                .contains("ADD COLUMN union_id VARCHAR(100)")
                .contains("ADD COLUMN corp_id VARCHAR(100)")
                .contains("ADD COLUMN last_synced_at DATETIME(3)")
                .contains("CREATE TABLE ding_employee_department")
                .contains("is_primary TINYINT UNSIGNED NOT NULL DEFAULT 0")
                .contains("CREATE TABLE ding_directory_sync_log")
                .contains("触发方式：SCHEDULED-每小时定时同步，MANUAL-接口手动触发")
                .contains("同步状态：RUNNING-进行中，SUCCESS-成功，FAILED-失败，SKIPPED-已有任务执行而跳过");
    }

    @Test
    void dingTalkDirectorySchedulerRunsAtEveryFullHourByDefault() throws IOException {
        Path scheduler = Path.of("src", "main", "java", "com", "saibao", "invoice", "config",
                "DingDirectorySyncScheduler.java");
        String source = Files.readString(scheduler, StandardCharsets.UTF_8);

        assertThat(source)
                .contains("@Scheduled(cron = \"${sebo.dingtalk.sync-cron:0 0 * * * *}\"")
                .contains("syncService.synchronize(\"SCHEDULED\", \"system\")");
    }

    @Test
    void multiOrganizationMigrationUsesCompositeDirectoryAndPermissionIdentities() throws IOException {
        Path migration = Path.of("src", "main", "resources", "db", "migration",
                "V104__support_multi_organization_dingtalk.sql");
        String sql = Files.readString(migration, StandardCharsets.UTF_8);

        assertThat(sql)
                .contains("uk_ding_employee_corp_user (corp_code, ding_user_id)")
                .contains("uk_ding_department_corp_id (corp_code, ding_department_id)")
                .contains("target_corp_code VARCHAR(50) NOT NULL DEFAULT 'default'")
                .contains("employee_id BIGINT UNSIGNED DEFAULT NULL");
    }
}
