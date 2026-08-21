package com.saibao.invoice.service;

import com.saibao.invoice.vo.QrTokenVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class QrTokenExpirationServiceTest {

    @Autowired private IQrTokenService qrTokenService;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void expiredQrTokenShouldReturnFriendlyBusinessMessage() {
        QrTokenVO token = qrTokenService.create(1L, 1L);
        jdbcTemplate.update(
                "UPDATE invoice_qr_token SET expires_at = ? WHERE token = ?",
                LocalDateTime.now().minusSeconds(1), token.getToken());

        assertThatThrownBy(() -> qrTokenService.resolve(token.getToken()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("二维码已过期，请重新获取二维码");
    }
}
