package com.saibao.invoice.service.impl;

import com.saibao.invoice.domain.OperationLog;
import com.saibao.invoice.dto.OperationLogPageQueryDTO;
import com.saibao.invoice.mapper.OperationLogMapper;
import com.saibao.invoice.service.IOperationLogService;
import com.saibao.invoice.vo.OperationLogVO;
import com.saibao.invoice.vo.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/** 操作日志查询实现。 */
@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements IOperationLogService {
    private final OperationLogMapper mapper;

    @Override
    public PageResult<OperationLogVO> page(OperationLogPageQueryDTO query) {
        long total = mapper.count(query);
        List<OperationLogVO> records = total == 0 ? Collections.emptyList() : mapper.selectPage(query).stream().map(this::toVO).toList();
        return new PageResult<>(records, total, query.getPageNum(), query.getPageSize());
    }

    private OperationLogVO toVO(OperationLog source) {
        OperationLogVO vo = new OperationLogVO();
        vo.setId(source.getId()); vo.setModuleType(source.getModuleType()); vo.setOperationType(source.getOperationType());
        vo.setBusinessId(source.getBusinessId()); vo.setBusinessName(source.getBusinessName()); vo.setDetailJson(source.getDetailJson());
        vo.setResult(source.getResult()); vo.setOperatorUserId(source.getOperatorUserId()); vo.setOperatorName(source.getOperatorName());
        vo.setClientIp(source.getClientIp()); vo.setCreatedAt(source.getCreatedAt());
        return vo;
    }
}
