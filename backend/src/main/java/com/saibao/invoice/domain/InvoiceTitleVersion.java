package com.saibao.invoice.domain;

import lombok.Data;

import java.time.LocalDateTime;

/** 发票抬头不可变版本快照领域对象。 */
@Data
public class InvoiceTitleVersion {
    private Long id;
    private Long titleId;
    private Integer versionNo;
    private String status;
    private String changeType;
    private String changeSummary;
    private String companyName;
    private String taxpayerId;
    private String registeredAddress;
    private String phone;
    private String bankName;
    private String bankAccount;
    private String subjectIdsJson;
    private String createdBy;
    private LocalDateTime createdAt;
}
