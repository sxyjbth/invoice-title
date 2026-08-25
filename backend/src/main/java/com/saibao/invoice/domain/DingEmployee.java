package com.saibao.invoice.domain;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 钉钉员工目录领域对象。 */
@Data
public class DingEmployee {
    private Long id;
    /** 企业业务编码，与 dingUserId 共同构成员工唯一身份。 */
    private String corpCode;
    /** 企业名称，用于目录检索和展示。 */
    private String corpName;
    private String corpId;
    private String dingUserId;
    private String unionId;
    private String employeeNo;
    private String employeeName;
    private Long departmentId;
    /** 员工所属的全部有效部门目录主键，包含主部门及兼职部门。 */
    private List<Long> departmentIds;
    private String departmentName;
    private String mobile;
    private String status;
    private LocalDateTime lastSyncedAt;
    /** 员工对查询主体的最终查看权限：true-启用，false-关闭。 */
    private Boolean permissionEnabled;
    private LocalDateTime updatedAt;
}
