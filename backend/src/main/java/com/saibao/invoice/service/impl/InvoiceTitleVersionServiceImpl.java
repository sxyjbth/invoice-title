package com.saibao.invoice.service.impl;

import com.saibao.invoice.domain.InvoiceTitleVersion;
import com.saibao.invoice.enums.InvoiceTitleStatusEnum;
import com.saibao.invoice.mapper.InvoiceTitleVersionMapper;
import com.saibao.invoice.service.IInvoiceTitleVersionService;
import com.saibao.invoice.util.BusinessTime;
import com.saibao.invoice.vo.InvoiceTitleVersionVO;
import com.saibao.invoice.vo.PageResult;
import com.saibao.invoice.dto.PageQueryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/** 历史版本恢复实现。 */
@Service
@RequiredArgsConstructor
public class InvoiceTitleVersionServiceImpl implements IInvoiceTitleVersionService {

    private final InvoiceTitleVersionMapper versionMapper;

    @Override
    @Transactional
    public Long restoreAsDraft(Long titleId, Long sourceVersionId, String operatorUserId) {
        InvoiceTitleVersion source = versionMapper.selectById(sourceVersionId);
        if (source == null || !source.getTitleId().equals(titleId)) {
            throw new IllegalArgumentException("历史版本不存在或不属于当前抬头");
        }

        // 恢复采用“复制快照并新建草稿”，保证线上已发布版本始终可追溯且不被覆盖。
        InvoiceTitleVersion draft = new InvoiceTitleVersion();
        draft.setTitleId(titleId);
        draft.setVersionNo(versionMapper.selectNextVersionNo(titleId));
        draft.setStatus(InvoiceTitleStatusEnum.DRAFT.getCode());
        draft.setCompanyName(source.getCompanyName());
        draft.setTaxpayerId(source.getTaxpayerId());
        draft.setRegisteredAddress(source.getRegisteredAddress());
        draft.setPhone(source.getPhone());
        draft.setBankName(source.getBankName());
        draft.setBankAccount(source.getBankAccount());
        draft.setCreatedBy(operatorUserId);
        draft.setCreatedAt(BusinessTime.now());
        versionMapper.insert(draft);
        return draft.getId();
    }

    @Override
    public InvoiceTitleVersionVO getCurrentPublishedVersion(Long titleId) {
        return toVO(required(versionMapper.selectCurrentPublished(titleId)));
    }

    @Override
    public InvoiceTitleVersionVO getVersion(Long versionId) {
        return toVO(required(versionMapper.selectById(versionId)));
    }

    @Override
    public PageResult<InvoiceTitleVersionVO> page(Long titleId, PageQueryDTO query) {
        long total = versionMapper.countByTitleId(titleId);
        List<InvoiceTitleVersionVO> records = total == 0
                ? Collections.emptyList()
                : versionMapper.selectPageByTitleId(titleId, query.getOffset(), query.getPageSize())
                .stream().map(this::toVO).toList();
        return new PageResult<>(records, total, query.getPageNum(), query.getPageSize());
    }

    private InvoiceTitleVersion required(InvoiceTitleVersion version) {
        if (version == null) {
            throw new IllegalArgumentException("抬头版本不存在");
        }
        return version;
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
