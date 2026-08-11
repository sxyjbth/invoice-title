package com.saibao.invoice.domain;

import lombok.Data;

import java.time.LocalDateTime;

/** 发票抬头当前态领域对象。 */
@Data
public class InvoiceTitle {
    private Long id;
    private String companyName;
    private String taxpayerId;
    private String registeredAddress;
    private String phone;
    private String bankName;
    private String bankAccount;
    private String status;
    private Long currentPublishedVersionId;
    private String subjectNames;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
