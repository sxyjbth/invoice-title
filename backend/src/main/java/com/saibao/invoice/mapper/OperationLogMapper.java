package com.saibao.invoice.mapper;

import com.saibao.invoice.domain.OperationLog;
import com.saibao.invoice.dto.OperationLogPageQueryDTO;

import java.util.List;

/** 操作日志持久化接口。 */
public interface OperationLogMapper {
    long count(OperationLogPageQueryDTO query);
    List<OperationLog> selectPage(OperationLogPageQueryDTO query);
    int insert(OperationLog log);
}
