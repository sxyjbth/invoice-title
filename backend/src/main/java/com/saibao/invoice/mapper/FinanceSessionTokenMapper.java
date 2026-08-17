package com.saibao.invoice.mapper;

import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/** 网页财务端页签会话令牌持久化接口。 */
public interface FinanceSessionTokenMapper {
    int insert(@Param("tokenHash") String tokenHash,
               @Param("financeUserId") Long financeUserId,
               @Param("expiresAt") LocalDateTime expiresAt,
               @Param("createdAt") LocalDateTime createdAt);

    Long selectActiveAccountId(@Param("tokenHash") String tokenHash,
                               @Param("now") LocalDateTime now);

    int deleteByTokenHash(@Param("tokenHash") String tokenHash);

    int deleteExpired(@Param("now") LocalDateTime now);
}
