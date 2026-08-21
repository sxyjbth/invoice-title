package com.saibao.invoice.service;

import com.saibao.invoice.dto.InvoiceTitlePageQueryDTO;
import com.saibao.invoice.dto.InvoiceTitleSaveDTO;
import com.saibao.invoice.enums.InvoiceTitleStatusEnum;
import com.saibao.invoice.vo.InvoiceTitleVO;
import com.saibao.invoice.vo.PageResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    @Transactional
    void publishingShouldRejectSubjectAlreadyUsedByAnotherPublishedTitle() {
        InvoiceTitleSaveDTO request = new InvoiceTitleSaveDTO();
        request.setCompanyName("主体冲突测试有限公司");
        request.setTaxpayerId("91330100SUBJECTCONFLICT");
        request.setRegisteredAddress("杭州市测试路 1 号");
        request.setPhone("0571-12345678");
        request.setBankName("测试银行杭州分行");
        request.setBankAccount("100000000001");
        request.setSubjectIds(List.of(1L));
        request.setStatus(InvoiceTitleStatusEnum.PUBLISHED.getCode());

        assertThatThrownBy(() -> invoiceTitleService.create(request, "finance-test"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("杭州主体")
                .hasMessageContaining("杭州赛宝卓越技术有限公司");
    }

    @Test
    @Transactional
    void draftShouldRejectSubjectAlreadyBoundToAnotherTitle() {
        InvoiceTitleSaveDTO request = new InvoiceTitleSaveDTO();
        request.setCompanyName("草稿主体冲突测试有限公司");
        request.setTaxpayerId("91330100DRAFTBIND001");
        request.setSubjectIds(List.of(1L));
        request.setStatus(InvoiceTitleStatusEnum.DRAFT.getCode());

        assertThatThrownBy(() -> invoiceTitleService.create(request, "finance-test"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("杭州主体")
                .hasMessageContaining("杭州赛宝卓越技术有限公司");
    }

    @Test
    @Transactional
    void draftShouldAllowSavingWithoutSubjects() {
        InvoiceTitleSaveDTO request = new InvoiceTitleSaveDTO();
        request.setCompanyName("待绑定主体测试有限公司");
        request.setTaxpayerId("91330100NOSUBJECTDRAFT");
        request.setSubjectIds(List.of());
        request.setStatus(InvoiceTitleStatusEnum.DRAFT.getCode());

        Long titleId = invoiceTitleService.create(request, "finance-test");

        assertThat(invoiceTitleService.getById(titleId).getSubjectIds()).isEmpty();
    }

    @Test
    @Transactional
    void draftUpdatedAtShouldUseShanghaiBusinessTimeWhenJvmRunsInUtc() {
        TimeZone originalTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        try {
            ZoneId businessZone = ZoneId.of("Asia/Shanghai");
            LocalDateTime beforeCreate = LocalDateTime.now(businessZone).minusSeconds(1);
            InvoiceTitleSaveDTO request = new InvoiceTitleSaveDTO();
            request.setCompanyName("草稿更新时间测试有限公司");
            request.setTaxpayerId("91330100DRAFTTIME001");
            request.setSubjectIds(List.of());
            request.setStatus(InvoiceTitleStatusEnum.DRAFT.getCode());

            Long titleId = invoiceTitleService.create(request, "finance-test");

            LocalDateTime afterCreate = LocalDateTime.now(businessZone).plusSeconds(1);
            assertThat(invoiceTitleService.getById(titleId).getUpdatedAt())
                    .isBetween(beforeCreate, afterCreate);
        } finally {
            TimeZone.setDefault(originalTimeZone);
        }
    }

    @Test
    @Transactional
    void publishingShouldRequireAtLeastOneSubject() {
        InvoiceTitleSaveDTO request = new InvoiceTitleSaveDTO();
        request.setCompanyName("无主体发布测试有限公司");
        request.setTaxpayerId("91330100NOSUBJECTPUB");
        request.setSubjectIds(List.of());
        request.setStatus(InvoiceTitleStatusEnum.PUBLISHED.getCode());

        assertThatThrownBy(() -> invoiceTitleService.create(request, "finance-test"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("发布抬头时请至少选择一个展示主体");
    }
}
