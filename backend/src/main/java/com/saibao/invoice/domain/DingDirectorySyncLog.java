package com.saibao.invoice.domain;

import lombok.Data;

import java.time.LocalDateTime;

/** 钉钉通讯录同步审计记录。 */
@Data
public class DingDirectorySyncLog {
    private Long id;
    private String triggerType;
    private String status;
    private Integer departmentCount;
    private Integer employeeCount;
    private String operatorName;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
