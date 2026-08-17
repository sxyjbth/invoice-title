package com.saibao.invoice.contract;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class SubjectPermissionMapperSqlContractTest {

    private static final Path MAPPER = Path.of(
            "src", "main", "resources", "mapper", "SubjectPermissionMapper.xml");

    @Test
    void effectiveEmployeeCountKeepsOuterEmployeeCorrelationOutOfJoinOnClause() throws IOException {
        String mapperXml = Files.readString(MAPPER, StandardCharsets.UTF_8);

        assertThat(mapperXml)
                .doesNotContain("ON employee_department.employee_id = e.id")
                .contains("WHERE employee_department.employee_id = e.id");
    }
}
