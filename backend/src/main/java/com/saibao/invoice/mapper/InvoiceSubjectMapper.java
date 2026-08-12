package com.saibao.invoice.mapper;

import com.saibao.invoice.domain.InvoiceSubject;
import com.saibao.invoice.dto.SubjectPageQueryDTO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 主体管理持久化接口。 */
public interface InvoiceSubjectMapper {
    long count(SubjectPageQueryDTO query);
    List<InvoiceSubject> selectPage(SubjectPageQueryDTO query);
    InvoiceSubject selectById(@Param("id") Long id);
    List<InvoiceSubject> selectByIds(@Param("ids") List<Long> ids);
    InvoiceSubject selectByCode(@Param("subjectCode") String subjectCode);
    InvoiceSubject selectByName(@Param("subjectName") String subjectName);
    int insert(InvoiceSubject subject);
    int update(InvoiceSubject subject);
    int updateStatus(@Param("id") Long id, @Param("status") String status, @Param("updatedBy") String updatedBy);
    int updateAllEmployeeVisible(@Param("id") Long id,
                                 @Param("allEmployeeVisible") boolean allEmployeeVisible,
                                 @Param("updatedBy") String updatedBy);
}
