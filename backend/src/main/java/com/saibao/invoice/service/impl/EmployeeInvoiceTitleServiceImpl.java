package com.saibao.invoice.service.impl;

import com.saibao.invoice.domain.InvoiceTitle;
import com.saibao.invoice.dto.EmployeeInvoiceTitlePageQueryDTO;
import com.saibao.invoice.mapper.EmployeeInvoiceTitleMapper;
import com.saibao.invoice.service.IEmployeeInvoiceTitleService;
import com.saibao.invoice.vo.InvoiceTitleVO;
import com.saibao.invoice.vo.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** 员工端抬头查询实现，权限判断在同一条 SQL 中完成，避免先查后过滤造成越权。 */
@Service
@RequiredArgsConstructor
public class EmployeeInvoiceTitleServiceImpl implements IEmployeeInvoiceTitleService {

    private final EmployeeInvoiceTitleMapper employeeInvoiceTitleMapper;

    @Override
    public PageResult<InvoiceTitleVO> pageAuthorized(EmployeeInvoiceTitlePageQueryDTO query, Long employeeId) {
        long total = employeeInvoiceTitleMapper.countAuthorized(query, employeeId);
        List<InvoiceTitleVO> records = total == 0 ? Collections.emptyList()
                : employeeInvoiceTitleMapper.selectAuthorizedPage(query, employeeId).stream().map(this::toVO).toList();
        return new PageResult<>(records, total, query.getPageNum(), query.getPageSize());
    }

    private InvoiceTitleVO toVO(InvoiceTitle title) {
        InvoiceTitleVO vo = new InvoiceTitleVO();
        vo.setId(title.getId());
        vo.setCompanyName(title.getCompanyName());
        vo.setTaxpayerId(title.getTaxpayerId());
        vo.setRegisteredAddress(title.getRegisteredAddress());
        vo.setPhone(title.getPhone());
        vo.setBankName(title.getBankName());
        vo.setBankAccount(title.getBankAccount());
        vo.setStatus(title.getStatus());
        vo.setUpdatedAt(title.getUpdatedAt());
        vo.setUpdatedBy(title.getUpdatedBy());
        vo.setSubjectNames(title.getSubjectNames() == null || title.getSubjectNames().isBlank()
                ? Collections.emptyList() : Arrays.stream(title.getSubjectNames().split(",")).map(String::trim).toList());
        return vo;
    }
}
