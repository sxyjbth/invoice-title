package com.saibao.invoice.service;

import com.saibao.invoice.dto.OperationLogPageQueryDTO;
import com.saibao.invoice.dto.SubjectPageQueryDTO;
import com.saibao.invoice.dto.SubjectPermissionPageQueryDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class AdministrationListServiceTest {

    @Autowired
    private IInvoiceSubjectService invoiceSubjectService;

    @Autowired
    private ISubjectPermissionService subjectPermissionService;

    @Autowired
    private IOperationLogService operationLogService;

    @Test
    void everyAdministrationListShouldUseServerSidePagination() {
        SubjectPageQueryDTO subjectQuery = new SubjectPageQueryDTO();
        subjectQuery.setPageNum(1);
        subjectQuery.setPageSize(20);
        assertThat(invoiceSubjectService.page(subjectQuery).getRecords())
                .extracting("subjectName").contains("杭州主体", "北京主体");

        SubjectPermissionPageQueryDTO permissionQuery = new SubjectPermissionPageQueryDTO();
        permissionQuery.setPageNum(1);
        permissionQuery.setPageSize(20);
        assertThat(subjectPermissionService.page(permissionQuery).getRecords())
                .extracting("targetName").contains("示例员工");

        OperationLogPageQueryDTO logQuery = new OperationLogPageQueryDTO();
        logQuery.setPageNum(1);
        logQuery.setPageSize(20);
        assertThat(operationLogService.page(logQuery).getRecords())
                .extracting("operationType").contains("PUBLISH");
    }
}
