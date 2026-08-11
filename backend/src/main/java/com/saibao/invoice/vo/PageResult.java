package com.saibao.invoice.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 统一服务端分页返回结构。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分页结果")
public class PageResult<T> {

    @Schema(description = "当前页数据")
    private List<T> records;

    @Schema(description = "符合条件的总条数", example = "14")
    private long total;

    @Schema(description = "当前页码", example = "1")
    private int pageNum;

    @Schema(description = "每页条数", example = "20")
    private int pageSize;
}
