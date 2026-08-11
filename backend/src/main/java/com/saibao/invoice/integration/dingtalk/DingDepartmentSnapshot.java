package com.saibao.invoice.integration.dingtalk;

/** 单次钉钉通讯录同步中的部门快照。 */
public record DingDepartmentSnapshot(
        String dingDepartmentId,
        String departmentName,
        String parentDepartmentId,
        int sortNo) {
}
