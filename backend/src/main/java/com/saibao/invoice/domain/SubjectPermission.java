package com.saibao.invoice.domain;

import lombok.Data;

import java.time.LocalDateTime;

/** 主体查看权限领域对象。 */
@Data
public class SubjectPermission {
    private Long id;
    private Long subjectId;
    private String subjectName;
    private String targetType;
    /** 授权对象所属企业业务编码；与 targetId 共同定位员工或部门。 */
    private String targetCorpCode;
    private String targetId;
    private String targetName;
    /** ALLOW-允许查看；历史 DENY 规则保留兼容但不再参与权限判定。 */
    private String permissionEffect;
    private Boolean includeChildDepartments;
    private String status;
    private String source;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
}
