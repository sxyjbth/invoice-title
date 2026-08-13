package com.saibao.invoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/** 发票抬头历史版本快照。 */
@Data
@Schema(description = "发票抬头版本信息")
public class InvoiceTitleVersionVO {

    @Schema(description = "版本主键 ID", example = "3")
    private Long id;

    @Schema(description = "所属抬头 ID", example = "1")
    private Long titleId;

    @Schema(description = "版本号，单个抬头内递增", example = "3")
    private Integer versionNo;

    @Schema(description = "版本状态：DRAFT-草稿，PUBLISHED-已发布，DISABLED-已停用")
    private String status;

    @Schema(description = "公司名称快照")
    private String companyName;

    @Schema(description = "纳税人识别号快照")
    private String taxpayerId;

    @Schema(description = "注册地址快照")
    private String registeredAddress;

    @Schema(description = "联系电话快照")
    private String phone;

    @Schema(description = "开户银行快照")
    private String bankName;

    @Schema(description = "银行账号快照")
    private String bankAccount;

    @Schema(description = "版本创建人姓名；财务账号无法解析时返回原操作人标识")
    private String createdBy;

    @Schema(description = "版本创建时间；发布版本即实际发布时间")
    private LocalDateTime createdAt;
}
