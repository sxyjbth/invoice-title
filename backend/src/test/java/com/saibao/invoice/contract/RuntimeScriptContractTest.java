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
    void infrastructureScriptWritesNonInteractiveNacosAuthenticationProperties() throws IOException {
        String infrastructure = readProjectFile("scripts", "start-infrastructure.ps1");

        assertThat(infrastructure)
                .contains("Set-NacosProperty")
                .contains("Set-NacosProperty $nacosProperties 'nacos.server.main.port' '28848'")
                .contains("Set-NacosProperty $nacosProperties 'nacos.console.port' '28081'")
                .contains("Assert-PortFree 28081 'Nacos Console'")
                .contains("Assert-PortFree 27848 'Nacos JRaft'")
                .contains("nacos.core.auth.server.identity.key")
                .contains("nacos.core.auth.server.identity.value")
                .contains("nacos.core.auth.plugin.nacos.token.secret.key");
    }

    @Test
    void localNacosRunsWithoutBootstrapCredentialsOrRepeatedLoginFailures() throws IOException {
        String infrastructure = readProjectFile("scripts", "start-infrastructure.ps1");
        String localConfiguration = readProjectFile(
                "backend", "src", "main", "resources", "application-local.yml");

        assertThat(infrastructure).contains("$env:NACOS_AUTH_ENABLE = 'false'");
        assertThat(localConfiguration)
                .doesNotContain("username: nacos")
                .doesNotContain("password: nacos");
    }

    @Test
    void productionReleaseIsOwnedByTheSystemdServiceAccount() throws IOException {
        String buildRelease = readProjectFile("deploy", "server-build-release.sh");

        assertThat(buildRelease)
                .contains("APP_USER=\"${INVOICE_APP_USER:-invoice_title}\"")
                .contains("chown -R \"${APP_USER}:${APP_GROUP}\" \"${staging_dir}\"");
    }

    private String readProjectFile(String... parts) throws IOException {
        Path backendRoot = Path.of("").toAbsolutePath();
        Path projectRoot = backendRoot.getParent();
        return Files.readString(projectRoot.resolve(
                Path.of(parts[0], Arrays.copyOfRange(parts, 1, parts.length))), StandardCharsets.UTF_8);
    }
}
