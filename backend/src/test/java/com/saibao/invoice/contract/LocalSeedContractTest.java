package com.saibao.invoice.contract;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class LocalSeedContractTest {

    private static final Path LOCAL_SEED = Path.of(
            "src", "main", "resources", "db", "local", "V100__seed_local_demo_data.sql");

    @Test
    void localSeedCreatesRequestedAdministratorWithBcryptPassword() throws IOException {
        String seed = Files.readString(LOCAL_SEED, StandardCharsets.UTF_8);

        assertThat(seed)
                .contains("INSERT INTO finance_user")
                .contains("'admin'")
                .contains("'SUPER_ADMIN'")
                .contains("'ENABLED'");

        Matcher hashMatcher = Pattern.compile("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}").matcher(seed);
        assertThat(hashMatcher.find()).isTrue();
        assertThat(new BCryptPasswordEncoder().matches("root", hashMatcher.group())).isTrue();
    }

    @Test
    void localSeedReplacesTheBootstrapAdministratorWithoutDeletingOtherAccounts() throws IOException {
        String seed = Files.readString(LOCAL_SEED, StandardCharsets.UTF_8);

        assertThat(seed)
                .contains("ON DUPLICATE KEY UPDATE")
                .doesNotContain("DELETE FROM finance_user");
    }

    @Test
    void localSeedCoversPublishedDraftAndDisabledTitleStates() throws IOException {
        String seed = Files.readString(LOCAL_SEED, StandardCharsets.UTF_8);

        assertThat(seed)
                .contains("'PUBLISHED'")
                .contains("'DRAFT'")
                .contains("'DISABLED'");
    }

    @Test
    void localDirectorySeedProvidesSearchableDepartmentsAndEmployees() throws IOException {
        Path directorySeed = Path.of(
                "src", "main", "resources", "db", "local", "V102__seed_ding_directory.sql");

        assertThat(directorySeed).exists();
        String seed = Files.readString(directorySeed, StandardCharsets.UTF_8);
        assertThat(seed)
                .contains("INSERT INTO ding_department")
                .contains("INSERT INTO ding_employee")
                .contains("'SB0001'")
                .contains("'技术中心'")
                .contains("'13800000001'");
    }
}
