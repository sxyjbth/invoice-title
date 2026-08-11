package com.saibao.invoice.service;

import com.saibao.invoice.dto.DepartmentDirectoryPageQueryDTO;
import com.saibao.invoice.dto.EmployeeDirectoryPageQueryDTO;
import com.saibao.invoice.vo.DingDepartmentVO;
import com.saibao.invoice.vo.DingEmployeeVO;
import com.saibao.invoice.vo.PageResult;

/** 财务端钉钉通讯录查询服务。 */
public interface IDingDirectoryService {
    PageResult<DingEmployeeVO> pageEmployees(EmployeeDirectoryPageQueryDTO query);
    PageResult<DingDepartmentVO> pageDepartments(DepartmentDirectoryPageQueryDTO query);
}
