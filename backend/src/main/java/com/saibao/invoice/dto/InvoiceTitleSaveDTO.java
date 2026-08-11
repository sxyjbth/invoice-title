package com.saibao.invoice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** 财务端新增或编辑发票抬头请求。 */
@Data
@Schema(description = "发票抬头保存请求")
public class InvoiceTitleSaveDTO {
    @NotBlank(message = "公司名称不能为空")
    @Size(max = 200, message = "公司名称不能超过 200 个字符")
    @Schema(description = "公司名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String companyName;

    @NotBlank(message = "纳税人识别号不能为空")
    @Size(max = 32, message = "纳税人识别号不能超过 32 个字符")
    @Schema(description = "纳税人识别号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String taxpayerId;

    @Size(max = 500, message = "注册地址不能超过 500 个字符")
    @Schema(description = "注册地址")
    private String registeredAddress;

    @Size(max = 50, message = "电话不能超过 50 个字符")
    @Schema(description = "联系电话")
    private String phone;

    @Size(max = 200, message = "开户行不能超过 200 个字符")
    @Schema(description = "开户银行")
    private String bankName;

    @Size(max = 64, message = "银行账号不能超过 64 个字符")
    @Schema(description = "银行账号，按字符串保存")
    private String bankAccount;

    @NotEmpty(message = "至少选择一个展示主体")
    @Schema(description = "展示主体 ID 列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<Long> subjectIds;

    @NotBlank(message = "抬头状态不能为空")
    @Pattern(regexp = "DRAFT|PUBLISHED", message = "保存状态只能是 DRAFT 或 PUBLISHED")
    @Schema(description = "保存状态：DRAFT-保存草稿，PUBLISHED-保存并发布", requiredMode = Schema.RequiredMode.REQUIRED)
    private String status;
}
