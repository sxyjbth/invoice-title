package com.saibao.invoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 按钉钉企业分组的已选员工。 */
@Data
@Schema(description = "按钉钉企业分组的已选员工")
public class EmployeeSelectionGroupVO {

    @Schema(description = "企业业务编码", example = "sebo")
    private String corpCode;

    @Schema(description = "企业名称", example = "赛宝绿创能源技术（上海）有限公司")
    private String corpName;

    @Schema(description = "该企业已选员工人数", example = "2")
    private Long employeeCount;

    @Schema(description = "该企业的已选在职员工")
    private List<DingEmployeeVO> employees = new ArrayList<>();
}
