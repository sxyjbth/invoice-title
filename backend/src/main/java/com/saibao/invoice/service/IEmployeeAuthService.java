package com.saibao.invoice.service;

import com.saibao.invoice.vo.DingEmployeeVO;
import com.saibao.invoice.vo.DingTalkOrganizationVO;

import java.util.List;

/** 钉钉员工免登身份校验服务。 */
public interface IEmployeeAuthService {
    DingEmployeeVO authenticate(String corpCode, String authCode);

    List<DingTalkOrganizationVO> listOrganizations();

    DingEmployeeVO getActiveEmployee(Long employeeId);
}
