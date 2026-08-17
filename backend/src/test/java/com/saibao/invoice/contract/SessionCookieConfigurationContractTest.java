package com.saibao.invoice.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class SessionCookieConfigurationContractTest {

    @Test
    void isolatesTheInvoiceApplicationSessionFromOtherProjectsOnTheSameHost() throws IOException {
        ClassPathResource resource = new ClassPathResource("application.yml");
        String config = resource.getContentAsString(StandardCharsets.UTF_8);

        assertThat(config)
            .contains("name: ${INVOICE_SESSION_COOKIE_NAME:INVOICE_TITLE_SESSION}");
    }
}
