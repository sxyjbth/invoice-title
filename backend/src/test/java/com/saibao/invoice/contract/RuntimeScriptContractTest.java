package com.saibao.invoice.contract;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class RuntimeScriptContractTest {

    @Test
    void startupScriptsNormalizeWindowsPathBeforeStartingChildProcesses() throws IOException {
        String infrastructure = readProjectFile("scripts", "start-infrastructure.ps1");
        String applications = readProjectFile("scripts", "start-applications.ps1");

        assertThat(infrastructure).contains("Normalize-ProcessPathEnvironment");
        assertThat(applications).contains("Normalize-ProcessPathEnvironment");
    }

    @Test
    void localRuntimeUsesExistingMysqlOn3306WithoutManagingIt() throws IOException {
        String bootstrap = readProjectFile("scripts", "bootstrap.ps1");
        String startAll = readProjectFile("scripts", "start-all.ps1");
        String infrastructure = readProjectFile("scripts", "start-infrastructure.ps1");
        String status = readProjectFile("scripts", "status.ps1");
        String stop = readProjectFile("scripts", "stop-all.ps1");

        assertThat(bootstrap.toLowerCase()).doesNotContain("nacos", "redis", "mysql-8.4.10");
        assertThat(infrastructure)
                .contains("existing MySQL")
                .doesNotContainIgnoringCase("mysqld")
                .doesNotContainIgnoringCase("nacos")
                .doesNotContainIgnoringCase("redis");
        assertThat(status).contains("else { 3306 }");
        assertThat(stop).doesNotContainIgnoringCase("mysqladmin");

        assertThat(String.join("\n", bootstrap, startAll, infrastructure, status, stop))
                .doesNotContain("23306", "UseLocalMySql");
    }

    @Test
    void productionReleaseIsOwnedByTheSystemdServiceAccount() throws IOException {
        String buildRelease = readProjectFile("deploy", "server-build-release.sh");

        assertThat(buildRelease)
                .contains("APP_USER=\"${INVOICE_APP_USER:-invoice_title}\"")
                .contains("chown -R \"${APP_USER}:${APP_GROUP}\" \"${staging_dir}\"");
    }

    @Test
    void productionReleaseKeepsPriorHashedAssetsForAlreadyOpenBrowserTabs() throws IOException {
        String buildRelease = readProjectFile("deploy", "server-build-release.sh");

        assertThat(buildRelease)
                .contains("copy_current_assets employee-h5")
                .contains("copy_current_assets finance-admin")
                .contains("previous_release=")
                .contains("copy_release_assets \"${previous_release}\" \"${app_name}\"");
        assertThat(buildRelease.indexOf("copy_current_assets finance-admin"))
                .isLessThan(buildRelease.indexOf(
                        "cp -a frontend/finance-admin/dist/. \"${staging_dir}/frontend/finance-admin/\""));
    }

    private String readProjectFile(String... parts) throws IOException {
        Path backendRoot = Path.of("").toAbsolutePath();
        Path projectRoot = backendRoot.getParent();
        return Files.readString(projectRoot.resolve(
                Path.of(parts[0], Arrays.copyOfRange(parts, 1, parts.length))), StandardCharsets.UTF_8);
    }
}
