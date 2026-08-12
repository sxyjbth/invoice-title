package com.saibao.invoice.service;

import com.saibao.invoice.dto.ImportTaskPageQueryDTO;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class InvoiceImportServiceTest {

    @Autowired
    private IInvoiceImportService invoiceImportService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldImportValidRowsAsDraftsAndRecordInvalidRows() throws Exception {
        MockMultipartFile file = workbookWithOneValidAndOneInvalidRow();

        var task = invoiceImportService.importWorkbook(file, "ding-user-finance", "王财务");

        assertThat(task.getStatus()).isEqualTo("PARTIAL_FAILED");
        assertThat(task.getTotalCount()).isEqualTo(2);
        assertThat(task.getSuccessCount()).isEqualTo(1);
        assertThat(task.getFailureCount()).isEqualTo(1);

        ImportTaskPageQueryDTO query = new ImportTaskPageQueryDTO();
        query.setPageNum(1);
        query.setPageSize(20);
        assertThat(invoiceImportService.page(query).getRecords())
                .extracting("originalFileName")
                .contains("invoice-title-import.xlsx");
    }

    @Test
    void templateAndImportShouldContainOnlyInvoiceTitleFieldsWithoutSubject() throws Exception {
        try (XSSFWorkbook template = new XSSFWorkbook(new ByteArrayInputStream(invoiceImportService.createTemplate()))) {
            var header = template.getSheetAt(0).getRow(0);
            assertThat(header.getLastCellNum()).isEqualTo((short) 6);
            assertThat(IntStream.range(0, header.getLastCellNum())
                    .mapToObj(index -> header.getCell(index).getStringCellValue()).toList())
                    .containsExactly("公司名称", "纳税人识别号", "注册地址", "电话", "开户行", "银行账号");
        }

        var task = invoiceImportService.importWorkbook(
                workbookWithOneValidAndOneInvalidRow(), "ding-user-finance", "王财务");
        assertThat(task.getSuccessCount()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM invoice_title_subject its
                INNER JOIN invoice_title t ON t.id = its.title_id
                WHERE t.taxpayer_id = '91310000IMPORT00001'
                """, Integer.class)).isZero();
    }

    private MockMultipartFile workbookWithOneValidAndOneInvalidRow() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("发票抬头");
            var header = sheet.createRow(0);
            String[] headers = {"公司名称", "纳税人识别号", "注册地址", "电话", "开户行", "银行账号"};
            for (int index = 0; index < headers.length; index++) {
                header.createCell(index).setCellValue(headers[index]);
            }

            var valid = sheet.createRow(1);
            String[] values = {"上海测试技术有限公司", "91310000IMPORT00001", "上海市浦东新区测试路1号", "021-88888888", "宁波银行上海支行", "86040000000000001"};
            for (int index = 0; index < values.length; index++) {
                valid.createCell(index).setCellValue(values[index]);
            }

            var invalid = sheet.createRow(2);
            invalid.createCell(0).setCellValue("缺少税号的公司");

            workbook.write(output);
            return new MockMultipartFile(
                    "file",
                    "invoice-title-import.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    output.toByteArray()
            );
        }
    }
}
