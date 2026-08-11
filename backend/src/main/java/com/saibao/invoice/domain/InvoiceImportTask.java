package com.saibao.invoice.domain;

import lombok.Data;

import java.time.LocalDateTime;

/** 发票抬头批量导入任务领域对象。 */
@Data
public class InvoiceImportTask {
    private Long id;
    private String taskNo;
    private String originalFileName;
    private String storageProvider;
    private String storageKey;
    private String status;
    private Integer totalCount;
    private Integer successCount;
    private Integer failureCount;
    private String errorFileKey;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String createdBy;
    private LocalDateTime createdAt;
}
