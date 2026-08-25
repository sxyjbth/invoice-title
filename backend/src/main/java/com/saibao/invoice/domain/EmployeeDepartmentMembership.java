package com.saibao.invoice.domain;

import lombok.Data;

/** 员工与其所属部门的目录关联，用于一次查询组装完整多部门归属。 */
@Data
public class EmployeeDepartmentMembership {
    private Long employeeId;
    private Long departmentId;
}
