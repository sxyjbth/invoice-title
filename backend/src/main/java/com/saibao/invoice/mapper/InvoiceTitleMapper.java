package com.saibao.invoice.mapper;

import com.saibao.invoice.domain.InvoiceTitle;
import com.saibao.invoice.domain.PublishedSubjectBinding;
import com.saibao.invoice.dto.InvoiceTitlePageQueryDTO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 发票抬头持久化接口，SQL 位于 resources/mapper。 */
public interface InvoiceTitleMapper {
    long count(InvoiceTitlePageQueryDTO query);
    List<InvoiceTitle> selectPage(InvoiceTitlePageQueryDTO query);
    InvoiceTitle selectById(@Param("id") Long id);
    InvoiceTitle selectByTaxpayerId(@Param("taxpayerId") String taxpayerId);
    List<Long> selectSubjectIds(@Param("titleId") Long titleId);
    List<Long> selectTitleIdsBySubjectId(@Param("subjectId") Long subjectId);
    List<PublishedSubjectBinding> selectPublishedSubjectBindings(
            @Param("subjectIds") List<Long> subjectIds,
            @Param("excludeTitleId") Long excludeTitleId);
    int insert(InvoiceTitle title);
    int update(InvoiceTitle title);
    int insertTitleSubject(@Param("titleId") Long titleId, @Param("subjectId") Long subjectId, @Param("createdBy") String createdBy);
    int deleteTitleSubjects(@Param("titleId") Long titleId);
    int deleteSubjectBindings(@Param("subjectId") Long subjectId);
    int updateCurrentPublishedVersion(@Param("id") Long id, @Param("versionId") Long versionId, @Param("updatedBy") String updatedBy);
    int updateStatus(@Param("id") Long id, @Param("status") String status, @Param("updatedBy") String updatedBy);
}
