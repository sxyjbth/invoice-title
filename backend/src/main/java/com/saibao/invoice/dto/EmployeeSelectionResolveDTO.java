package com.saibao.invoice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 将企业、部门和员工目录选择统一解析为最终员工集合的请求。 */
@Data
@Schema(description = "企业、部门和员工选择的批量解析请求")
public class EmployeeSelectionResolveDTO {

    @Size(max = 100, message = "企业选择不能超过 100 个")
    @Schema(description = "选中的企业业务编码；每个企业会解析为该企业全部在职员工", example = "[\"sebo\",\"walden\"]")
    private List<@NotBlank(message = "企业业务编码不能为空") String> corpCodes = new ArrayList<>();

    @Size(max = 5000, message = "部门选择不能超过 5000 个")
    @Schema(description = "选中的部门目录主键 ID；每个部门会解析为其全部在职员工")
    private List<@Positive(message = "部门目录主键 ID 必须大于 0") Long> departmentIds = new ArrayList<>();

    @Size(max = 5000, message = "员工选择不能超过 5000 个")
    @Schema(description = "单独选中的员工目录主键 ID")
    private List<@Positive(message = "员工目录主键 ID 必须大于 0") Long> employeeIds = new ArrayList<>();
}
