package com.saibao.invoice.service;

import com.saibao.invoice.vo.DingDirectorySyncResultVO;

/** 钉钉通讯录同步服务。 */
public interface IDingDirectorySyncService {
    /**
     * 同步部门、员工及员工多部门关系。
     *
     * @param triggerType 触发类型：MANUAL-手动，SCHEDULED-定时
     * @param operatorName 触发账号；定时任务传 system
     * @return 同步统计结果
     */
    DingDirectorySyncResultVO synchronize(String triggerType, String operatorName);
}
