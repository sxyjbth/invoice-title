package com.saibao.invoice.mapper;

import com.saibao.invoice.domain.InvoiceImportRowError;
import com.saibao.invoice.domain.InvoiceImportTask;
import com.saibao.invoice.dto.ImportRowErrorPageQueryDTO;
import com.saibao.invoice.dto.ImportTaskPageQueryDTO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/** 批量导入任务及失败行持久化接口。 */
public interface InvoiceImportTaskMapper {
    long count(ImportTaskPageQueryDTO query);
    List<InvoiceImportTask> selectPage(ImportTaskPageQueryDTO query);
    int insert(InvoiceImportTask task);
    int updateResult(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("totalCount") int totalCount,
                     @Param("successCount") int successCount,
                     @Param("failureCount") int failureCount,
                     @Param("finishedAt") LocalDateTime finishedAt);
    int insertRowError(InvoiceImportRowError rowError);
    long countRowErrors(ImportRowErrorPageQueryDTO query);
    List<InvoiceImportRowError> selectRowErrorPage(ImportRowErrorPageQueryDTO query);
}
