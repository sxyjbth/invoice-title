package com.saibao.invoice.service.impl;

import com.saibao.invoice.domain.DingDepartment;
import com.saibao.invoice.domain.DingEmployee;
import com.saibao.invoice.dto.DepartmentDirectoryPageQueryDTO;
import com.saibao.invoice.dto.EmployeeDirectoryPageQueryDTO;
import com.saibao.invoice.mapper.DingDirectoryMapper;
import com.saibao.invoice.service.IDingDirectoryService;
import com.saibao.invoice.vo.DingDepartmentVO;
import com.saibao.invoice.vo.DingEmployeeVO;
import com.saibao.invoice.vo.DingOrganizationVO;
import com.saibao.invoice.vo.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/** 基于本地钉钉同步目录的分页查询实现。 */
@Service
@RequiredArgsConstructor
public class DingDirectoryServiceImpl implements IDingDirectoryService {
    private final DingDirectoryMapper mapper;

    @Override
    public List<DingOrganizationVO> listOrganizations() {
        return mapper.selectOrganizations();
    }

    @Override
    public PageResult<DingEmployeeVO> pageEmployees(EmployeeDirectoryPageQueryDTO query) {
        long total = mapper.countEmployees(query);
        var records = total == 0 ? Collections.<DingEmployeeVO>emptyList()
                : mapper.selectEmployeePage(query).stream().map(this::toEmployeeVO).toList();
        return new PageResult<>(records, total, query.getPageNum(), query.getPageSize());
    }

    @Override
    public PageResult<DingDepartmentVO> pageDepartments(DepartmentDirectoryPageQueryDTO query) {
        long total = mapper.countDepartments(query);
        var records = total == 0 ? Collections.<DingDepartmentVO>emptyList()
                : mapper.selectDepartmentPage(query).stream().map(this::toDepartmentVO).toList();
        return new PageResult<>(records, total, query.getPageNum(), query.getPageSize());
    }

    private DingEmployeeVO toEmployeeVO(DingEmployee source) {
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
        vo.setPermissionEnabled(source.getPermissionEnabled());
        return vo;
    }

    private DingDepartmentVO toDepartmentVO(DingDepartment source) {
        DingDepartmentVO vo = new DingDepartmentVO();
        vo.setId(source.getId());
        vo.setCorpCode(source.getCorpCode());
        vo.setCorpName(source.getCorpName());
        vo.setDingDepartmentId(source.getDingDepartmentId());
        vo.setDepartmentName(source.getDepartmentName());
        vo.setParentDepartmentId(source.getParentDepartmentId());
        vo.setStatus(source.getStatus());
        vo.setEmployeeCount(source.getEmployeeCount());
        return vo;
    }
}
