package com.saibao.invoice.integration.dingtalk;

import java.util.List;

/** 单个钉钉企业完整拉取完成后的部门与员工快照。 */
public record DingOrganizationDirectorySnapshot(
        String corpCode,
        String corpName,
        String corpId,
        List<DingDepartmentSnapshot> departments,
        List<DingEmployeeSnapshot> employees) {
}
