package com.saibao.invoice.domain;

import lombok.Data;

import java.time.LocalDateTime;

/** 财务关键操作审计日志领域对象。 */
@Data
public class OperationLog {
    private Long id;
    private String moduleType;
    private String operationType;
    private String businessId;
    private String businessName;
    private String detailJson;
    private String result;
    private String operatorUserId;
    private String operatorName;
    private String clientIp;
    private LocalDateTime createdAt;
}
