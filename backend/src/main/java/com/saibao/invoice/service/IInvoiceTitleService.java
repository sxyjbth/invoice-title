package com.saibao.invoice.service;

import com.saibao.invoice.dto.InvoiceTitlePageQueryDTO;
import com.saibao.invoice.dto.InvoiceTitleSaveDTO;
import com.saibao.invoice.vo.InvoiceTitleVO;
import com.saibao.invoice.vo.PageResult;

/** 发票抬头业务服务。 */
public interface IInvoiceTitleService {
    PageResult<InvoiceTitleVO> page(InvoiceTitlePageQueryDTO query);
    InvoiceTitleVO getById(Long id);
    Long create(InvoiceTitleSaveDTO request, String operatorUserId);
    void update(Long id, InvoiceTitleSaveDTO request, String operatorUserId);
    void disable(Long id, String operatorUserId);
}
