package com.saibao.invoice.service.impl;

import com.saibao.invoice.config.DingTalkProperties;
import com.saibao.invoice.domain.DingEmployee;
import com.saibao.invoice.integration.dingtalk.DingTalkClient;
import com.saibao.invoice.integration.dingtalk.DingTalkIdentity;
import com.saibao.invoice.mapper.DingDirectoryMapper;
import com.saibao.invoice.service.IEmployeeAuthService;
import com.saibao.invoice.vo.DingEmployeeVO;
import com.saibao.invoice.vo.DingTalkOrganizationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** 免登码必须在指定企业内校验，并按 corpCode + userId 匹配在职员工。 */
@Service
@RequiredArgsConstructor
public class EmployeeAuthServiceImpl implements IEmployeeAuthService {
    private final DingTalkClient dingTalkClient;
    private final DingDirectoryMapper mapper;
    private final DingTalkProperties properties;

    @Override
    public DingEmployeeVO authenticate(String corpCode, String authCode) {
        DingTalkIdentity identity = dingTalkClient.resolveIdentity(corpCode, authCode);
        DingEmployee employee = mapper.selectEmployeeByIdentity(corpCode, identity.dingUserId());
        if (employee == null) {
            throw new SecurityException("当前钉钉员工未同步或已离职，请联系管理员同步通讯录");
        }
        return toVO(employee);
    }

    @Override
    public List<DingTalkOrganizationVO> listOrganizations() {
        return properties.activeOrganizations().stream()
                .map(item -> new DingTalkOrganizationVO(item.getCorpCode(), item.getCorpName(), item.getCorpId()))
                .toList();
    }

    @Override
    public DingEmployeeVO getActiveEmployee(Long employeeId) {
        DingEmployee employee = mapper.selectActiveEmployeeById(employeeId);
        if (employee == null) {
            throw new SecurityException("员工已离职或停用，请重新登录");
        }
        return toVO(employee);
    }

    private DingEmployeeVO toVO(DingEmployee source) {
        DingEmployeeVO vo = new DingEmployeeVO();
        vo.setId(source.getId());
        vo.setCorpCode(source.getCorpCode());
        vo.setCorpName(source.getCorpName());
        vo.setDingUserId(source.getDingUserId());
        vo.setEmployeeNo(source.getEmployeeNo());
        vo.setEmployeeName(source.getEmployeeName());
        vo.setDepartmentId(source.getDepartmentId());
        vo.setDepartmentName(source.getDepartmentName());
        vo.setMobile(source.getMobile());
        vo.setStatus(source.getStatus());
        return vo;
    }
}
