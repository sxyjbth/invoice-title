package com.saibao.invoice.service;

import com.saibao.invoice.vo.InvoiceTitleVersionVO;
import com.saibao.invoice.vo.QrTokenVO;

/** 临时二维码签发与解析服务。 */
public interface IQrTokenService {
    QrTokenVO create(Long titleId, Long employeeId);
    InvoiceTitleVersionVO resolve(String token);
}
