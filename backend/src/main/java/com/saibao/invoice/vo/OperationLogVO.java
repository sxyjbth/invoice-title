package com.saibao.invoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/** 操作日志列表与详情数据。 */
@Data
@Schema(description = "操作日志信息")
public class OperationLogVO {
    @Schema(description = "日志主键 ID") private Long id;
    @Schema(description = "业务模块代码") private String moduleType;
    @Schema(description = "操作类型代码") private String operationType;
    @Schema(description = "业务对象 ID") private String businessId;
    @Schema(description = "业务对象名称") private String businessName;
    @Schema(description = "操作明细 JSON") private String detailJson;
    @Schema(description = "执行结果：SUCCESS-成功，FAILED-失败") private String result;
    @Schema(description = "操作人的钉钉用户 ID") private String operatorUserId;
    @Schema(description = "操作人姓名") private String operatorName;
    @Schema(description = "客户端 IP") private String clientIp;
    @Schema(description = "操作时间") private LocalDateTime createdAt;
}
