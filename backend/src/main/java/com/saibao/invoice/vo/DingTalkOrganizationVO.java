package com.saibao.invoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;

/** 员工端申请钉钉免登码所需的非敏感企业信息。 */
@Schema(description = "已接入的钉钉企业")
public record DingTalkOrganizationVO(
        @Schema(description = "企业业务编码", example = "sebo") String corpCode,
        @Schema(description = "企业名称", example = "赛宝绿创能源技术（上海）有限公司") String corpName,
        @Schema(description = "钉钉 corpId；前端 requestAuthCode 使用") String corpId) {
}
