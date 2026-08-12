package com.saibao.invoice.domain;

import lombok.Data;

import java.time.LocalDateTime;

/** 发票展示主体领域对象。 */
@Data
public class InvoiceSubject {
    private Long id;
    private String subjectCode;
    private String subjectName;
    private String status;
    /** 1-全部在职员工可见，0-按部门和员工规则计算。 */
    private Boolean allEmployeeVisible;
    private Integer sortNo;
    private Long employeeCount;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
}
