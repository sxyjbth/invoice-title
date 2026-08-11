package com.saibao.invoice.domain;

import lombok.Data;

import java.time.LocalDateTime;

/** 发票抬头导入失败行领域对象。 */
@Data
public class InvoiceImportRowError {
    private Long id;
    private Long taskId;
    private Integer rowNo;
    private String taxpayerId;
    private String errorCode;
    private String errorMessage;
    private String rawDataJson;
    private LocalDateTime createdAt;
}
