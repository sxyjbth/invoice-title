package com.saibao.invoice.controller;

import com.saibao.invoice.dto.ImportRowErrorPageQueryDTO;
import com.saibao.invoice.dto.ImportTaskPageQueryDTO;
import com.saibao.invoice.service.IInvoiceImportService;
import com.saibao.invoice.vo.ImportRowErrorVO;
import com.saibao.invoice.vo.ImportTaskVO;
import com.saibao.invoice.vo.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

/** 财务端批量导入及导入历史接口。 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/invoice-imports")
@Tag(name = "财务端-批量导入", description = "Excel 模板下载、抬头导入、导入历史与失败行查询")
public class InvoiceImportController {

    private final IInvoiceImportService invoiceImportService;

    @GetMapping
    @Operation(summary = "分页查询导入历史", description = "导入历史统一采用服务端分页")
    public PageResult<ImportTaskVO> page(@Valid ImportTaskPageQueryDTO query) {
        return invoiceImportService.page(query);
    }

    @GetMapping("/errors")
    @Operation(summary = "分页查询导入失败行", description = "按任务分页返回 Excel 行号、错误码和修正说明")
    public PageResult<ImportRowErrorVO> pageErrors(@Valid ImportRowErrorPageQueryDTO query) {
        return invoiceImportService.pageErrors(query);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "导入发票抬头", description = "仅支持 .xlsx，最多 1,000 行；有效行创建草稿，失败行单独留痕")
    public ImportTaskVO upload(
            @Parameter(description = "待导入的 .xlsx 文件", required = true)
            @RequestPart("file") MultipartFile file,
            @Parameter(description = "操作人的钉钉用户 ID", required = true, example = "ding-user-finance")
            @RequestParam String operatorUserId,
            @Parameter(description = "操作人姓名", required = true, example = "王财务")
            @RequestParam String operatorName) {
        return invoiceImportService.importWorkbook(file, operatorUserId, operatorName);
    }

    @GetMapping("/template")
    @Operation(summary = "下载导入模板", description = "下载带表头及一条示例数据的 Excel 模板")
    public ResponseEntity<byte[]> downloadTemplate() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("invoice-title-import-template.xlsx", StandardCharsets.UTF_8)
                .build());
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        return ResponseEntity.ok().headers(headers).body(invoiceImportService.createTemplate());
    }
}
