CREATE TABLE finance_session_token (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '财务端页签会话主键 ID',
    token_hash CHAR(64) NOT NULL COMMENT '随机会话令牌的 SHA-256 十六进制摘要，禁止保存或记录原始令牌',
    finance_user_id BIGINT UNSIGNED NOT NULL COMMENT '当前页签已登录的财务端账号 ID',
    expires_at DATETIME(3) NOT NULL COMMENT '会话绝对过期时间；超过该时间后必须重新登录',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '会话创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_finance_session_token_hash (token_hash),
    KEY idx_finance_session_account_expire (finance_user_id, expires_at),
    KEY idx_finance_session_expire (expires_at),
    CONSTRAINT fk_finance_session_account FOREIGN KEY (finance_user_id) REFERENCES finance_user (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='网页财务端按浏览器页签隔离的登录会话表';
