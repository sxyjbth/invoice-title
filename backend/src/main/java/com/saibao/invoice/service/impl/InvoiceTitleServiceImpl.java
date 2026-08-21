package com.saibao.invoice.service.impl;

import com.saibao.invoice.domain.InvoiceTitle;
import com.saibao.invoice.domain.InvoiceSubject;
import com.saibao.invoice.domain.InvoiceTitleVersion;
import com.saibao.invoice.dto.InvoiceTitlePageQueryDTO;
import com.saibao.invoice.dto.InvoiceTitleSaveDTO;
import com.saibao.invoice.enums.InvoiceTitleStatusEnum;
import com.saibao.invoice.mapper.InvoiceTitleMapper;
import com.saibao.invoice.mapper.InvoiceTitleVersionMapper;
import com.saibao.invoice.service.IInvoiceTitleService;
import com.saibao.invoice.util.BusinessTime;
import com.saibao.invoice.vo.InvoiceTitleVO;
import com.saibao.invoice.vo.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

/** 发票抬头查询实现。 */
@Service
@RequiredArgsConstructor
public class InvoiceTitleServiceImpl implements IInvoiceTitleService {

    private final InvoiceTitleMapper invoiceTitleMapper;
    private final InvoiceTitleVersionMapper versionMapper;
    private final InvoiceTitleSubjectBindingManager bindingManager;

    @Override
    public PageResult<InvoiceTitleVO> page(InvoiceTitlePageQueryDTO query) {
        long total = invoiceTitleMapper.count(query);
        List<InvoiceTitleVO> records = total == 0
                ? Collections.emptyList()
                : invoiceTitleMapper.selectPage(query).stream().map(title -> toVO(title, false)).toList();
        return new PageResult<>(records, total, query.getPageNum(), query.getPageSize());
    }

    @Override
    public InvoiceTitleVO getById(Long id) {
        InvoiceTitle title = invoiceTitleMapper.selectById(id);
        if (title == null) {
            throw new IllegalArgumentException("发票抬头不存在：" + id);
        }
        return toVO(title, true);
    }

    @Override
    @Transactional
    public Long create(InvoiceTitleSaveDTO request, String operatorUserId) {
        ensureTaxpayerUnique(request.getTaxpayerId(), null);
        InvoiceSubject subject = bindingManager.requireAvailableSubject(
                request.getSubjectIds(), null, isPublished(request));
        List<InvoiceSubject> subjects = subject == null ? Collections.emptyList() : List.of(subject);
        List<Long> subjectIds = subject == null ? Collections.emptyList() : List.of(subject.getId());
        InvoiceTitle title = new InvoiceTitle();
        applyRequest(title, request, subjects, operatorUserId);
        title.setCreatedBy(operatorUserId);
        title.setCreatedAt(BusinessTime.now());
        invoiceTitleMapper.insert(title);
        bindingManager.replaceTitleBinding(title.getId(), subject, operatorUserId);
        createVersion(title, subjectIds, "CREATE", operatorUserId);
        return title.getId();
    }

    @Override
    @Transactional
    public void update(Long id, InvoiceTitleSaveDTO request, String operatorUserId) {
        InvoiceTitle title = invoiceTitleMapper.selectByIdForUpdate(id);
        if (title == null) throw new IllegalArgumentException("发票抬头不存在：" + id);
        ensureTaxpayerUnique(request.getTaxpayerId(), id);
        InvoiceSubject subject = bindingManager.requireAvailableSubject(
                request.getSubjectIds(), id, isPublished(request));
        List<InvoiceSubject> subjects = subject == null ? Collections.emptyList() : List.of(subject);
        List<Long> subjectIds = subject == null ? Collections.emptyList() : List.of(subject.getId());
        applyRequest(title, request, subjects, operatorUserId);
        if (invoiceTitleMapper.update(title) == 0) throw new IllegalArgumentException("发票抬头不存在：" + id);
        bindingManager.replaceTitleBinding(id, subject, operatorUserId);
        createVersion(title, subjectIds,
                "PUBLISHED".equals(request.getStatus()) ? "PUBLISH" : "EDIT", operatorUserId);
    }

    @Override
    public void disable(Long id, String operatorUserId) {
        int updated = invoiceTitleMapper.updateStatus(id, InvoiceTitleStatusEnum.DISABLED.getCode(), operatorUserId);
        if (updated == 0) {
            throw new IllegalArgumentException("发票抬头不存在：" + id);
        }
    }

    private void ensureTaxpayerUnique(String taxpayerId, Long currentId) {
        InvoiceTitle duplicate = invoiceTitleMapper.selectByTaxpayerId(taxpayerId.trim());
        if (duplicate != null && (currentId == null || !duplicate.getId().equals(currentId))) {
            throw new IllegalArgumentException("纳税人识别号已存在：" + taxpayerId);
        }
    }

    private boolean isPublished(InvoiceTitleSaveDTO request) {
        return InvoiceTitleStatusEnum.PUBLISHED.getCode().equals(request.getStatus());
    }

    private void applyRequest(InvoiceTitle title, InvoiceTitleSaveDTO request,
                              List<InvoiceSubject> subjects, String operatorUserId) {
        title.setCompanyName(request.getCompanyName().trim());
        title.setTaxpayerId(request.getTaxpayerId().trim());
        title.setRegisteredAddress(trimToNull(request.getRegisteredAddress()));
        title.setPhone(trimToNull(request.getPhone()));
        title.setBankName(trimToNull(request.getBankName()));
        title.setBankAccount(trimToNull(request.getBankAccount()));
        title.setStatus(request.getStatus());
        title.setSubjectNames(subjects.stream().map(InvoiceSubject::getSubjectName)
                .reduce((left, right) -> left + "," + right).orElse(""));
        title.setUpdatedBy(operatorUserId);
        title.setUpdatedAt(BusinessTime.now());
    }

    private void createVersion(InvoiceTitle title, List<Long> subjectIds, String changeType, String operatorUserId) {
        InvoiceTitleVersion version = new InvoiceTitleVersion();
        version.setTitleId(title.getId());
        version.setVersionNo(versionMapper.selectNextVersionNo(title.getId()));
        version.setStatus(title.getStatus());
        version.setChangeType(changeType);
        version.setChangeSummary("PUBLISHED".equals(title.getStatus()) ? "财务保存并发布" : "财务保存草稿");
        version.setCompanyName(title.getCompanyName());
        version.setTaxpayerId(title.getTaxpayerId());
        version.setRegisteredAddress(title.getRegisteredAddress());
        version.setPhone(title.getPhone());
        version.setBankName(title.getBankName());
        version.setBankAccount(title.getBankAccount());
        version.setSubjectIdsJson(new LinkedHashSet<>(subjectIds).toString());
        version.setCreatedBy(operatorUserId);
        version.setCreatedAt(BusinessTime.now());
        versionMapper.insert(version);
        if ("PUBLISHED".equals(title.getStatus())) {
            invoiceTitleMapper.updateCurrentPublishedVersion(title.getId(), version.getId(), operatorUserId);
            title.setCurrentPublishedVersionId(version.getId());
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private InvoiceTitleVO toVO(InvoiceTitle title, boolean includeSubjectIds) {
        InvoiceTitleVO vo = new InvoiceTitleVO();
        vo.setId(title.getId());
        vo.setCompanyName(title.getCompanyName());
        vo.setTaxpayerId(title.getTaxpayerId());
        vo.setRegisteredAddress(title.getRegisteredAddress());
        vo.setPhone(title.getPhone());
        vo.setBankName(title.getBankName());
        vo.setBankAccount(title.getBankAccount());
        vo.setStatus(title.getStatus());
        vo.setUpdatedAt(title.getUpdatedAt());
        vo.setUpdatedBy(title.getUpdatedBy());
        vo.setSubjectNames(title.getSubjectNames() == null || title.getSubjectNames().isBlank()
                ? Collections.emptyList()
                : Arrays.stream(title.getSubjectNames().split(",")).map(String::trim).toList());
        if (includeSubjectIds) vo.setSubjectIds(invoiceTitleMapper.selectSubjectIds(title.getId()));
        return vo;
    }
}
