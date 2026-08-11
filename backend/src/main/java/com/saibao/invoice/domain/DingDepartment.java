package com.saibao.invoice.domain;

import lombok.Data;

import java.time.LocalDateTime;

/** 钉钉部门目录领域对象。 */
@Data
public class DingDepartment {
    private Long id;
    /** 企业业务编码，例如 sebo、walden。 */
    private String corpCode;
    /** 企业名称，用于财务端区分同名部门。 */
    private String corpName;
    private String corpId;
    private String dingDepartmentId;
    private String departmentName;
    private String parentDepartmentId;
    private String status;
    private Integer sortNo;
    private Long employeeCount;
    private LocalDateTime lastSyncedAt;
    private LocalDateTime updatedAt;
}
