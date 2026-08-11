package com.saibao.invoice.service;

import com.saibao.invoice.dto.ImportRowErrorPageQueryDTO;
import com.saibao.invoice.dto.ImportTaskPageQueryDTO;
import com.saibao.invoice.vo.ImportRowErrorVO;
import com.saibao.invoice.vo.ImportTaskVO;
import com.saibao.invoice.vo.PageResult;
import org.springframework.web.multipart.MultipartFile;

/** 发票抬头批量导入服务。 */
public interface IInvoiceImportService {
    PageResult<ImportTaskVO> page(ImportTaskPageQueryDTO query);
    PageResult<ImportRowErrorVO> pageErrors(ImportRowErrorPageQueryDTO query);
    ImportTaskVO importWorkbook(MultipartFile file, String operatorUserId, String operatorName);
    byte[] createTemplate();
}
