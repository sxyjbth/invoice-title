package com.saibao.invoice.integration.dingtalk;

import java.util.List;

/** 单次钉钉通讯录同步中的员工快照。 */
public record DingEmployeeSnapshot(
        String dingUserId,
        String unionId,
        String employeeNo,
        String employeeName,
        List<String> departmentIds,
        String mobile,
        boolean active) {
}
