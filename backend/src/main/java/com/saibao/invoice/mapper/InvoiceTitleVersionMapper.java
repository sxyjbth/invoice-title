package com.saibao.invoice.mapper;

import com.saibao.invoice.domain.InvoiceTitleVersion;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/** 发票抬头版本持久化接口。 */
public interface InvoiceTitleVersionMapper {
    InvoiceTitleVersion selectById(@Param("id") Long id);
    InvoiceTitleVersion selectCurrentPublished(@Param("titleId") Long titleId);
    int selectNextVersionNo(@Param("titleId") Long titleId);
    int insert(InvoiceTitleVersion version);
    long countByTitleId(@Param("titleId") Long titleId);
    List<InvoiceTitleVersion> selectPageByTitleId(@Param("titleId") Long titleId,
                                                  @Param("offset") int offset,
                                                  @Param("pageSize") int pageSize);
}
