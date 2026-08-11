package com.saibao.invoice.service;

import com.saibao.invoice.dto.EmployeeInvoiceTitlePageQueryDTO;
import com.saibao.invoice.vo.InvoiceTitleVO;
import com.saibao.invoice.vo.PageResult;

/** 员工端有权抬头查询服务。 */
public interface IEmployeeInvoiceTitleService {
    PageResult<InvoiceTitleVO> pageAuthorized(EmployeeInvoiceTitlePageQueryDTO query, Long employeeId);
}
