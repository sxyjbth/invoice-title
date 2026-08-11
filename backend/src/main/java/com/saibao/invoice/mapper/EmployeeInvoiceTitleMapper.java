package com.saibao.invoice.mapper;

import com.saibao.invoice.domain.InvoiceTitle;
import com.saibao.invoice.dto.EmployeeInvoiceTitlePageQueryDTO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 员工端基于主体权限的抬头查询。 */
public interface EmployeeInvoiceTitleMapper {
    long countAuthorized(@Param("query") EmployeeInvoiceTitlePageQueryDTO query, @Param("employeeId") Long employeeId);
    List<InvoiceTitle> selectAuthorizedPage(@Param("query") EmployeeInvoiceTitlePageQueryDTO query,
                                            @Param("employeeId") Long employeeId);
    boolean hasTitleAccess(@Param("titleId") Long titleId, @Param("employeeId") Long employeeId);
}
