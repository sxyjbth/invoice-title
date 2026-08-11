package com.saibao.invoice.service;

import com.saibao.invoice.dto.InvoiceTitlePageQueryDTO;
import com.saibao.invoice.enums.InvoiceTitleStatusEnum;
import com.saibao.invoice.vo.InvoiceTitleVO;
import com.saibao.invoice.vo.PageResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class InvoiceTitleCoreServiceTest {

    @Autowired
    private IInvoiceTitleService invoiceTitleService;

    @Autowired
    private IInvoiceTitleVersionService invoiceTitleVersionService;

    @Test
    void shouldFilterDraftTitlesWithServerSidePagination() {
        InvoiceTitlePageQueryDTO query = new InvoiceTitlePageQueryDTO();
        query.setPageNum(1);
        query.setPageSize(20);
        query.setStatus(InvoiceTitleStatusEnum.DRAFT.getCode());

        PageResult<InvoiceTitleVO> result = invoiceTitleService.page(query);

        assertThat(result.getTotal()).isEqualTo(1);
        assertThat(result.getRecords())
                .extracting(InvoiceTitleVO::getCompanyName)
                .containsExactly("北京示例技术服务有限公司");
    }

    @Test
    void restoringHistoryShouldCreateNewDraftWithoutReplacingPublishedVersion() {
        Long restoredVersionId = invoiceTitleVersionService.restoreAsDraft(1L, 1L, "ding-user-finance");

        assertThat(restoredVersionId).isNotNull();
        assertThat(invoiceTitleVersionService.getCurrentPublishedVersion(1L).getVersionNo()).isEqualTo(3);
        assertThat(invoiceTitleVersionService.getVersion(restoredVersionId).getStatus())
                .isEqualTo(InvoiceTitleStatusEnum.DRAFT.getCode());
    }
}

