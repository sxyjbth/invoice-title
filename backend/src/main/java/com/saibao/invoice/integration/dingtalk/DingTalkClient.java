package com.saibao.invoice.integration.dingtalk;

import java.util.List;

/** 隔离钉钉开放平台调用，便于测试同步和免登逻辑。 */
public interface DingTalkClient {
    DingTalkIdentity resolveIdentity(String authCode);

    default DingTalkIdentity resolveIdentity(String corpCode, String authCode) {
        return resolveIdentity(authCode);
    }

    List<DingDepartmentSnapshot> listDepartments();

    List<DingEmployeeSnapshot> listEmployees();

    /**
     * 依次获取所有企业的完整目录。旧单企业测试实现无需改造即可继续使用。
     */
    default List<DingOrganizationDirectorySnapshot> listDirectories() {
        return List.of(new DingOrganizationDirectorySnapshot(
                "default", "默认钉钉企业", corpId(), listDepartments(), listEmployees()));
    }

    default String corpId() {
        return null;
    }
}
