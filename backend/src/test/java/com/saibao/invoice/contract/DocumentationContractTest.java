package com.saibao.invoice.contract;

import com.saibao.invoice.controller.InvoiceTitleController;
import com.saibao.invoice.dto.InvoiceTitlePageQueryDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentationContractTest {

    @Test
    void controllerAndPublicContractFieldsShouldHaveSwaggerDescriptions() {
        assertThat(InvoiceTitleController.class.getAnnotation(Tag.class)).isNotNull();
        assertThat(Arrays.stream(InvoiceTitleController.class.getDeclaredMethods()))
                .allMatch(method -> method.getAnnotation(Operation.class) != null);

        for (Field field : InvoiceTitlePageQueryDTO.class.getDeclaredFields()) {
            Schema schema = field.getAnnotation(Schema.class);
            assertThat(schema)
                    .as("字段 %s 必须有 @Schema 注释", field.getName())
                    .isNotNull();
            assertThat(schema.description()).isNotBlank();
        }
    }
}

