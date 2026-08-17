package com.saibao.invoice.service.impl;

import com.saibao.invoice.mapper.FinanceSessionTokenMapper;
import com.saibao.invoice.service.IFinanceSessionTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/** 使用不可枚举的随机令牌实现财务端页签级会话。数据库仅保存令牌摘要。 */
@Service
@RequiredArgsConstructor
public class FinanceSessionTokenServiceImpl implements IFinanceSessionTokenService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;
    private static final int SESSION_HOURS = 8;

    private final FinanceSessionTokenMapper financeSessionTokenMapper;

    @Override
    @Transactional
    public String create(Long financeUserId) {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        LocalDateTime now = LocalDateTime.now();
        // 登录时顺带清理过期会话，避免令牌表长期无界增长。
        financeSessionTokenMapper.deleteExpired(now);
        financeSessionTokenMapper.insert(hash(rawToken), financeUserId, now.plusHours(SESSION_HOURS), now);
        return rawToken;
    }

    @Override
    public Long resolveAccountId(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return null;
        }
        return financeSessionTokenMapper.selectActiveAccountId(hash(rawToken), LocalDateTime.now());
    }

    @Override
    @Transactional
    public void invalidate(String rawToken) {
        if (rawToken != null && !rawToken.isBlank()) {
            financeSessionTokenMapper.deleteByTokenHash(hash(rawToken));
        }
    }

    private String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", exception);
        }
    }
}
