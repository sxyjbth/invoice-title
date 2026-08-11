package com.saibao.invoice.mapper;

import com.saibao.invoice.domain.QrToken;
import org.apache.ibatis.annotations.Param;

/** 临时二维码令牌持久化接口。 */
public interface QrTokenMapper {
    int insert(QrToken token);
    QrToken selectByToken(@Param("token") String token);
}
