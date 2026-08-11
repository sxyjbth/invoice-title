package com.saibao.invoice.controller;

import com.saibao.invoice.dto.OperationLogPageQueryDTO;
import com.saibao.invoice.service.IOperationLogService;
import com.saibao.invoice.vo.OperationLogVO;
import com.saibao.invoice.vo.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 财务端不可变操作审计日志接口。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/operation-logs")
@Tag(name = "财务端-操作日志", description = "分页检索发票抬头系统关键操作记录")
public class OperationLogController {
    private final IOperationLogService service;

    @GetMapping
    @Operation(summary = "分页查询操作日志")
    public PageResult<OperationLogVO> page(@Valid OperationLogPageQueryDTO query) {
        return service.page(query);
    }
}
