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
        InvoiceSubject duplicate = mapper.selectByCode(request.getSubjectCode().trim().toUpperCase());
        if (duplicate != null) throw new IllegalArgumentException("主体编码已存在：" + request.getSubjectCode());
        InvoiceSubject subject = new InvoiceSubject();
        subject.setSubjectCode(request.getSubjectCode().trim().toUpperCase());
        subject.setSubjectName(request.getSubjectName().trim());
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
        InvoiceSubject duplicate = mapper.selectByCode(request.getSubjectCode().trim().toUpperCase());
        if (duplicate != null && !duplicate.getId().equals(id)) {
            throw new IllegalArgumentException("主体编码已存在：" + request.getSubjectCode());
        }
        current.setSubjectCode(request.getSubjectCode().trim().toUpperCase());
        current.setSubjectName(request.getSubjectName().trim());
        current.setStatus(request.getStatus());
        current.setSortNo(request.getSortNo());
        current.setUpdatedBy(request.getOperatorUserId());
        if (mapper.update(current) == 0) throw new IllegalArgumentException("主体不存在：" + id);
    }

    @Override
    public void changeStatus(Long id, String status, String operatorUserId) {
        if (mapper.updateStatus(id, status, operatorUserId) == 0) throw new IllegalArgumentException("主体不存在：" + id);
    }

    private InvoiceSubjectVO toVO(InvoiceSubject source) {
        InvoiceSubjectVO vo = new InvoiceSubjectVO();
        vo.setId(source.getId()); vo.setSubjectCode(source.getSubjectCode()); vo.setSubjectName(source.getSubjectName());
        vo.setStatus(source.getStatus()); vo.setSortNo(source.getSortNo()); vo.setTitleCount(source.getTitleCount());
        vo.setEmployeeCount(source.getEmployeeCount()); vo.setUpdatedBy(source.getUpdatedBy()); vo.setUpdatedAt(source.getUpdatedAt());
        return vo;
    }
}
