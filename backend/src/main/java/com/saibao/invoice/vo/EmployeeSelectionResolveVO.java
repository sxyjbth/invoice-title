package com.saibao.invoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 企业、部门和员工选择解析后的最终员工集合。 */
@Data
@Schema(description = "企业、部门和员工选择解析结果")
public class EmployeeSelectionResolveVO {

    @Schema(description = "去重后的已选在职员工人数", example = "3")
    private Long selectedEmployeeCount = 0L;

    @Schema(description = "去重后的已选在职员工目录主键 ID")
    private List<Long> selectedEmployeeIds = new ArrayList<>();

    @Schema(description = "去重后的已选在职员工明细")
    private List<DingEmployeeVO> selectedEmployees = new ArrayList<>();

    @Schema(description = "按企业分组的已选在职员工")
    private List<EmployeeSelectionGroupVO> employeeGroups = new ArrayList<>();
}
