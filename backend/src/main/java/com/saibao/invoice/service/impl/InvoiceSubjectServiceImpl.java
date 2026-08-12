package com.saibao.invoice.service.impl;

import com.saibao.invoice.domain.InvoiceSubject;
import com.saibao.invoice.dto.InvoiceSubjectSaveDTO;
import com.saibao.invoice.dto.SubjectPageQueryDTO;
import com.saibao.invoice.mapper.InvoiceSubjectMapper;
import com.saibao.invoice.service.IInvoiceSubjectService;
import com.saibao.invoice.vo.InvoiceSubjectVO;
import com.saibao.invoice.vo.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** 主体管理服务实现。 */
@Service
@RequiredArgsConstructor
public class InvoiceSubjectServiceImpl implements IInvoiceSubjectService {
    private final InvoiceSubjectMapper mapper;

    @Override
    public PageResult<InvoiceSubjectVO> page(SubjectPageQueryDTO query) {
        long total = mapper.count(query);
        List<InvoiceSubjectVO> records = total == 0 ? Collections.emptyList() : mapper.selectPage(query).stream().map(this::toVO).toList();
        return new PageResult<>(records, total, query.getPageNum(), query.getPageSize());
    }

    @Override
    public Long create(InvoiceSubjectSaveDTO request) {
        String subjectName = request.getSubjectName().trim();
        if (mapper.selectByName(subjectName) != null) {
            throw new IllegalArgumentException("主体名称已存在：" + subjectName);
        }
        String subjectCode = normalizeCode(request.getSubjectCode());
        if (subjectCode == null) subjectCode = generateSubjectCode();
        InvoiceSubject duplicate = mapper.selectByCode(subjectCode);
        if (duplicate != null) throw new IllegalArgumentException("主体编码已存在：" + subjectCode);
        InvoiceSubject subject = new InvoiceSubject();
        subject.setSubjectCode(subjectCode);
        subject.setSubjectName(subjectName);
        subject.setStatus(request.getStatus());
        subject.setSortNo(request.getSortNo());
        subject.setCreatedBy(request.getOperatorUserId());
        subject.setUpdatedBy(request.getOperatorUserId());
        subject.setCreatedAt(LocalDateTime.now());
        subject.setUpdatedAt(LocalDateTime.now());
        mapper.insert(subject);
        return subject.getId();
    }

    @Override
    public void update(Long id, InvoiceSubjectSaveDTO request) {
        InvoiceSubject current = mapper.selectById(id);
        if (current == null) throw new IllegalArgumentException("主体不存在：" + id);
        String subjectName = request.getSubjectName().trim();
        InvoiceSubject duplicateName = mapper.selectByName(subjectName);
        if (duplicateName != null && !duplicateName.getId().equals(id)) {
            throw new IllegalArgumentException("主体名称已存在：" + subjectName);
        }
        String subjectCode = normalizeCode(request.getSubjectCode());
        if (subjectCode != null) {
            InvoiceSubject duplicate = mapper.selectByCode(subjectCode);
            if (duplicate != null && !duplicate.getId().equals(id)) {
                throw new IllegalArgumentException("主体编码已存在：" + subjectCode);
            }
            current.setSubjectCode(subjectCode);
        }
        current.setSubjectName(subjectName);
        current.setStatus(request.getStatus());
        current.setSortNo(request.getSortNo());
        current.setUpdatedBy(request.getOperatorUserId());
        if (mapper.update(current) == 0) throw new IllegalArgumentException("主体不存在：" + id);
    }

    @Override
    public void changeStatus(Long id, String status, String operatorUserId) {
        if (mapper.updateStatus(id, status, operatorUserId) == 0) throw new IllegalArgumentException("主体不存在：" + id);
    }

    private String normalizeCode(String subjectCode) {
        if (subjectCode == null || subjectCode.isBlank()) return null;
        return subjectCode.trim().toUpperCase(Locale.ROOT);
    }

    private String generateSubjectCode() {
        String subjectCode;
        do {
            subjectCode = "SUB-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
        } while (mapper.selectByCode(subjectCode) != null);
        return subjectCode;
    }

    private InvoiceSubjectVO toVO(InvoiceSubject source) {
        InvoiceSubjectVO vo = new InvoiceSubjectVO();
        vo.setId(source.getId()); vo.setSubjectCode(source.getSubjectCode()); vo.setSubjectName(source.getSubjectName());
        vo.setStatus(source.getStatus()); vo.setSortNo(source.getSortNo());
        vo.setEmployeeCount(source.getEmployeeCount()); vo.setUpdatedBy(source.getUpdatedBy()); vo.setUpdatedAt(source.getUpdatedAt());
        return vo;
    }
}
