package com.saibao.invoice.service;

import com.saibao.invoice.dto.InvoiceSubjectSaveDTO;
import com.saibao.invoice.dto.SubjectTitleBindingDTO;
import com.saibao.invoice.dto.SubjectPageQueryDTO;
import com.saibao.invoice.vo.InvoiceSubjectVO;
import com.saibao.invoice.vo.PageResult;

/** 主体管理业务服务。 */
public interface IInvoiceSubjectService {
    PageResult<InvoiceSubjectVO> page(SubjectPageQueryDTO query);
    Long create(InvoiceSubjectSaveDTO request);
    void update(Long id, InvoiceSubjectSaveDTO request);
    void changeStatus(Long id, String status, String operatorUserId);
    void bindTitle(Long id, SubjectTitleBindingDTO request);
}
