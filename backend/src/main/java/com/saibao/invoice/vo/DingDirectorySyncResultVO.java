package com.saibao.invoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/** 钉钉通讯录同步执行结果。 */
@Data
@Schema(description = "钉钉通讯录同步执行结果")
public class DingDirectorySyncResultVO {
    @Schema(description = "同步日志主键 ID") private Long syncLogId;
    @Schema(description = "触发方式：SCHEDULED-定时，MANUAL-手动") private String triggerType;
    @Schema(description = "执行状态：SUCCESS-成功，FAILED-失败，SKIPPED-跳过") private String status;
    @Schema(description = "同步部门数") private int departmentCount;
    @Schema(description = "同步员工数") private int employeeCount;
    @Schema(description = "执行说明；成功时为空") private String message;
    @Schema(description = "开始时间") private LocalDateTime startedAt;
    @Schema(description = "结束时间") private LocalDateTime finishedAt;
}
