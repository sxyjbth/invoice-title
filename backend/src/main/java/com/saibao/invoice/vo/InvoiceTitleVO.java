package com.saibao.invoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 发票抬头列表及员工展示数据。 */
@Data
@Schema(description = "发票抬头信息")
public class InvoiceTitleVO {

    @Schema(description = "抬头主键 ID", example = "1")
    private Long id;

    @Schema(description = "公司名称", example = "杭州赛宝卓越技术有限公司")
    private String companyName;

    @Schema(description = "纳税人识别号", example = "91110400MADFF1HE1T")
    private String taxpayerId;

    @Schema(description = "注册地址")
    private String registeredAddress;

    @Schema(description = "联系电话", example = "4008696096")
    private String phone;

    @Schema(description = "开户银行")
    private String bankName;

    @Schema(description = "银行账号")
    private String bankAccount;

    @Schema(description = "状态：DRAFT-草稿，PUBLISHED-已发布，DISABLED-已停用")
    private String status;

    @Schema(description = "展示主体名称列表")
    private List<String> subjectNames;

    @Schema(description = "展示主体 ID 列表，用于财务端编辑回显")
    private List<Long> subjectIds;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "更新人姓名")
    private String updatedBy;
}
