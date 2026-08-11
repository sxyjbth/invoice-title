package com.saibao.invoice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 操作日志分页查询条件。 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "操作日志分页查询条件")
public class OperationLogPageQueryDTO extends PageQueryDTO {
    @Schema(description = "业务模块：TITLE-抬头，SUBJECT-主体，PERMISSION-权限，IMPORT-导入，QR-二维码")
    private String moduleType;

    @Schema(description = "操作类型，例如 CREATE、UPDATE、PUBLISH、DISABLE、RESTORE、IMPORT")
    private String operationType;

    @Schema(description = "业务对象名称或操作人关键字")
    private String keyword;

    @Schema(description = "执行结果：SUCCESS-成功，FAILED-失败")
    private String result;
}
