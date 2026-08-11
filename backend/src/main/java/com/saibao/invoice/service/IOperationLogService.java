package com.saibao.invoice.service;

import com.saibao.invoice.dto.OperationLogPageQueryDTO;
import com.saibao.invoice.vo.OperationLogVO;
import com.saibao.invoice.vo.PageResult;

/** 操作审计日志查询服务。 */
public interface IOperationLogService {
    PageResult<OperationLogVO> page(OperationLogPageQueryDTO query);
}
