package com.saibao.invoice.service;

import com.saibao.invoice.dto.DepartmentDirectoryPageQueryDTO;
import com.saibao.invoice.dto.EmployeeDirectoryPageQueryDTO;
import com.saibao.invoice.vo.DingDepartmentVO;
import com.saibao.invoice.vo.DingEmployeeVO;
import com.saibao.invoice.vo.DingOrganizationVO;
import com.saibao.invoice.vo.PageResult;

import java.util.List;

/** 财务端钉钉通讯录查询服务。 */
public interface IDingDirectoryService {
    List<DingOrganizationVO> listOrganizations();
    PageResult<DingEmployeeVO> pageEmployees(EmployeeDirectoryPageQueryDTO query);
    PageResult<DingDepartmentVO> pageDepartments(DepartmentDirectoryPageQueryDTO query);
}
