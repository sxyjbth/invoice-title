package com.saibao.invoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** 财务端通讯录企业筛选选项。 */
@Data
@Schema(description = "钉钉通讯录企业选项")
public class DingOrganizationVO {
    @Schema(description = "企业业务编码", example = "sebo")
    private String corpCode;

    @Schema(description = "企业名称", example = "赛宝绿创能源技术（上海）有限公司")
    private String corpName;
}
