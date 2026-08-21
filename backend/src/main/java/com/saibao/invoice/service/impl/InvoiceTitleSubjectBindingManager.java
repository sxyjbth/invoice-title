package com.saibao.invoice.service.impl;

import com.saibao.invoice.domain.InvoiceSubject;
import com.saibao.invoice.domain.InvoiceTitle;
import com.saibao.invoice.mapper.InvoiceSubjectMapper;
import com.saibao.invoice.mapper.InvoiceTitleMapper;
import com.saibao.invoice.util.BusinessTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 统一维护发票抬头与主体的零或一对零或一关系。
 *
 * <p>普通抬头保存只允许选择未被其他抬头占用的主体；主体管理中的显式绑定操作允许换绑，
 * 但会同步清理关系两端的旧绑定并刷新受影响抬头的主体名称快照。</p>
 */
@Component
@RequiredArgsConstructor
public class InvoiceTitleSubjectBindingManager {

    private final InvoiceTitleMapper invoiceTitleMapper;
    private final InvoiceSubjectMapper invoiceSubjectMapper;

    /**
     * 校验抬头保存请求并锁定唯一目标主体，防止并发保存绕过一对一约束。
     */
    public InvoiceSubject requireAvailableSubject(List<Long> subjectIds,
                                                  Long currentTitleId,
                                                  boolean required) {
        if (subjectIds == null || subjectIds.isEmpty()) {
            if (required) throw new IllegalArgumentException("发布抬头时请至少选择一个展示主体");
            return null;
        }
        if (subjectIds.size() > 1) {
            throw new IllegalArgumentException("一个发票抬头只能绑定一个主体");
        }

        Long subjectId = subjectIds.get(0);
        List<InvoiceSubject> subjects = invoiceSubjectMapper.selectByIdsForUpdate(List.of(subjectId));
        if (subjects.size() != 1) throw new IllegalArgumentException("存在无效的展示主体");
        InvoiceSubject subject = subjects.get(0);
        if (!"ENABLED".equals(subject.getStatus())) {
            throw new IllegalArgumentException("已停用主体不能用于发票抬头展示");
        }

        List<Long> occupiedTitleIds = invoiceTitleMapper.selectTitleIdsBySubjectId(subjectId);
        Long occupiedTitleId = occupiedTitleIds.stream()
                .filter(titleId -> currentTitleId == null || !currentTitleId.equals(titleId))
                .findFirst()
                .orElse(null);
        if (occupiedTitleId != null) {
            InvoiceTitle occupiedTitle = invoiceTitleMapper.selectById(occupiedTitleId);
            String occupiedTitleName = occupiedTitle == null ? String.valueOf(occupiedTitleId) : occupiedTitle.getCompanyName();
            throw new IllegalArgumentException("主体“" + subject.getSubjectName()
                    + "”已绑定抬头“" + occupiedTitleName + "”，请先在主体管理中调整绑定关系");
        }
        return subject;
    }

    /**
     * 保存抬头时替换该抬头自身的绑定。目标主体已经由 requireAvailableSubject 锁定并校验。
     */
    public void replaceTitleBinding(Long titleId, InvoiceSubject subject, String operatorUserId) {
        invoiceTitleMapper.deleteTitleSubjects(titleId);
        if (subject != null) {
            invoiceTitleMapper.insertTitleSubject(titleId, subject.getId(), operatorUserId);
        }
        refreshSubjectNames(titleId, operatorUserId);
    }

    /**
     * 主体管理中的显式换绑：目标抬头和目标主体两侧的历史关系都会被替换。
     */
    public void forceRebind(Long titleId, Long subjectId, String operatorUserId) {
        InvoiceTitle selectedTitle = invoiceTitleMapper.selectByIdForUpdate(titleId);
        if (selectedTitle == null) throw new IllegalArgumentException("发票抬头不存在：" + titleId);
        if ("DISABLED".equals(selectedTitle.getStatus())) {
            throw new IllegalArgumentException("已停用发票抬头不能绑定主体");
        }

        List<InvoiceSubject> subjects = invoiceSubjectMapper.selectByIdsForUpdate(List.of(subjectId));
        if (subjects.size() != 1) throw new IllegalArgumentException("主体不存在：" + subjectId);
        InvoiceSubject selectedSubject = subjects.get(0);
        if (!"ENABLED".equals(selectedSubject.getStatus())) {
            throw new IllegalArgumentException("已停用主体不能绑定发票抬头");
        }

        Set<Long> affectedTitleIds = new LinkedHashSet<>(invoiceTitleMapper.selectTitleIdsBySubjectId(subjectId));
        affectedTitleIds.add(titleId);

        // 两侧都先清理再写入，数据库唯一键负责兜住并发情况下的一对一约束。
        invoiceTitleMapper.deleteSubjectBindings(subjectId);
        invoiceTitleMapper.deleteTitleSubjects(titleId);
        invoiceTitleMapper.insertTitleSubject(titleId, subjectId, operatorUserId);
        affectedTitleIds.forEach(affectedTitleId -> refreshSubjectNames(affectedTitleId, operatorUserId));
    }

    /**
     * 主体改名后同步所有关联抬头的展示名称快照。
     *
     * <p>这里只更新冗余名称，不改抬头业务更新时间、操作人或发布版本，避免员工端将主体改名
     * 误展示成抬头内容更新。</p>
     */
    public void synchronizeSubjectNameSnapshot(Long subjectId, String subjectName) {
        invoiceTitleMapper.updateSubjectNameSnapshotBySubjectId(subjectId, subjectName);
    }

    private void refreshSubjectNames(Long titleId, String operatorUserId) {
        InvoiceTitle title = invoiceTitleMapper.selectById(titleId);
        if (title == null) return;
        List<Long> subjectIds = invoiceTitleMapper.selectSubjectIds(titleId);
        List<InvoiceSubject> boundSubjects = subjectIds.isEmpty()
                ? Collections.emptyList()
                : invoiceSubjectMapper.selectByIds(subjectIds);
        title.setSubjectNames(boundSubjects.stream()
                .map(InvoiceSubject::getSubjectName)
                .findFirst()
                .orElse(""));
        title.setUpdatedBy(operatorUserId);
        title.setUpdatedAt(BusinessTime.now());
        if (invoiceTitleMapper.update(title) == 0) {
            throw new IllegalArgumentException("发票抬头不存在：" + titleId);
        }
    }
}
