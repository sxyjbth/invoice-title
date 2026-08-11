package com.saibao.invoice.domain;

import lombok.Data;

import java.time.LocalDateTime;

/** 网页财务端登录账号领域对象。 */
@Data
public class FinanceUser {
    private Long id;
    private String username;
    private String displayName;
    private String passwordHash;
    private String roleType;
    private String status;
    private LocalDateTime passwordChangedAt;
    private LocalDateTime lastLoginAt;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
}
