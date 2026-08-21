package com.saibao.invoice.service.impl;

import com.saibao.invoice.domain.InvoiceTitle;
import com.saibao.invoice.domain.InvoiceTitleVersion;
import com.saibao.invoice.domain.QrToken;
import com.saibao.invoice.enums.InvoiceTitleStatusEnum;
import com.saibao.invoice.exception.QrTokenExpiredException;
import com.saibao.invoice.mapper.EmployeeInvoiceTitleMapper;
import com.saibao.invoice.mapper.InvoiceTitleMapper;
import com.saibao.invoice.mapper.InvoiceTitleVersionMapper;
import com.saibao.invoice.mapper.QrTokenMapper;
import com.saibao.invoice.service.IQrTokenService;
import com.saibao.invoice.vo.InvoiceTitleVersionVO;
import com.saibao.invoice.vo.QrTokenVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/** 临时二维码服务实现。 */
@Service
@RequiredArgsConstructor
public class QrTokenServiceImpl implements IQrTokenService {

    private static final int VALID_MINUTES = 10;

    private final QrTokenMapper qrTokenMapper;
    private final InvoiceTitleMapper invoiceTitleMapper;
    private final InvoiceTitleVersionMapper versionMapper;
    private final EmployeeInvoiceTitleMapper employeeInvoiceTitleMapper;

    @Override
    public QrTokenVO create(Long titleId, Long employeeId) {
        InvoiceTitle title = invoiceTitleMapper.selectById(titleId);
        if (title == null || !InvoiceTitleStatusEnum.PUBLISHED.getCode().equals(title.getStatus())) {
            throw new IllegalStateException("抬头未发布，不能生成二维码");
        }
        if (!employeeInvoiceTitleMapper.hasTitleAccess(titleId, employeeId)) {
            throw new IllegalStateException("当前员工没有该抬头的查看权限");
        }
        InvoiceTitleVersion published = versionMapper.selectCurrentPublished(titleId);
        if (published == null) {
            throw new IllegalStateException("抬头缺少已发布版本");
        }

        LocalDateTime now = LocalDateTime.now();
        QrToken token = new QrToken();
        token.setToken(UUID.randomUUID().toString().replace("-", ""));
        token.setTitleId(titleId);
        token.setVersionId(published.getId());
        token.setEmployeeId(employeeId);
        token.setCreatedAt(now);
        token.setExpiresAt(now.plusMinutes(VALID_MINUTES));
        qrTokenMapper.insert(token);
        return new QrTokenVO(token.getToken(), "/?qrToken=" + token.getToken(), token.getExpiresAt());
    }

    @Override
    public InvoiceTitleVersionVO resolve(String rawToken) {
        QrToken token = qrTokenMapper.selectByToken(rawToken);
        if (token == null || !token.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new QrTokenExpiredException();
        }
        InvoiceTitle title = invoiceTitleMapper.selectById(token.getTitleId());
        boolean active = title != null
                && InvoiceTitleStatusEnum.PUBLISHED.getCode().equals(title.getStatus())
                && token.getEmployeeId() != null
                && employeeInvoiceTitleMapper.hasTitleAccess(token.getTitleId(), token.getEmployeeId());
        if (!active) {
            throw new IllegalStateException("抬头、主体或查看权限已变更，二维码已失效");
        }
        InvoiceTitleVersion snapshot = versionMapper.selectById(token.getVersionId());
        if (snapshot == null) {
            throw new IllegalStateException("二维码对应的发布版本已失效");
        }
        return toVO(snapshot);
    }

    private InvoiceTitleVersionVO toVO(InvoiceTitleVersion version) {
        InvoiceTitleVersionVO vo = new InvoiceTitleVersionVO();
        vo.setId(version.getId());
        vo.setTitleId(version.getTitleId());
        vo.setVersionNo(version.getVersionNo());
        vo.setStatus(version.getStatus());
        vo.setCompanyName(version.getCompanyName());
        vo.setTaxpayerId(version.getTaxpayerId());
        vo.setRegisteredAddress(version.getRegisteredAddress());
        vo.setPhone(version.getPhone());
        vo.setBankName(version.getBankName());
        vo.setBankAccount(version.getBankAccount());
        vo.setCreatedBy(version.getCreatedBy());
        vo.setCreatedAt(version.getCreatedAt());
        return vo;
    }
}
