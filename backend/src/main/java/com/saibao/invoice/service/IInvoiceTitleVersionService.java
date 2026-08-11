package com.saibao.invoice.service;

import com.saibao.invoice.vo.InvoiceTitleVersionVO;
import com.saibao.invoice.vo.PageResult;
import com.saibao.invoice.dto.PageQueryDTO;

/** 发票抬头历史版本服务。 */
public interface IInvoiceTitleVersionService {
    Long restoreAsDraft(Long titleId, Long sourceVersionId, String operatorUserId);
    InvoiceTitleVersionVO getCurrentPublishedVersion(Long titleId);
    InvoiceTitleVersionVO getVersion(Long versionId);
    PageResult<InvoiceTitleVersionVO> page(Long titleId, PageQueryDTO query);
}
