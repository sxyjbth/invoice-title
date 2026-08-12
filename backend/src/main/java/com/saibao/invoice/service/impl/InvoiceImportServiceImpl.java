package com.saibao.invoice.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saibao.invoice.domain.InvoiceImportRowError;
import com.saibao.invoice.domain.InvoiceImportTask;
import com.saibao.invoice.domain.InvoiceTitle;
import com.saibao.invoice.domain.InvoiceTitleVersion;
import com.saibao.invoice.domain.OperationLog;
import com.saibao.invoice.dto.ImportRowErrorPageQueryDTO;
import com.saibao.invoice.dto.ImportTaskPageQueryDTO;
import com.saibao.invoice.enums.InvoiceTitleStatusEnum;
import com.saibao.invoice.mapper.InvoiceImportTaskMapper;
import com.saibao.invoice.mapper.InvoiceTitleMapper;
import com.saibao.invoice.mapper.InvoiceTitleVersionMapper;
import com.saibao.invoice.mapper.OperationLogMapper;
import com.saibao.invoice.service.IInvoiceImportService;
import com.saibao.invoice.vo.ImportRowErrorVO;
import com.saibao.invoice.vo.ImportTaskVO;
import com.saibao.invoice.vo.PageResult;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Excel 批量导入实现；所有成功数据先生成草稿，必须由财务复核后发布。 */
@Service
@RequiredArgsConstructor
public class InvoiceImportServiceImpl implements IInvoiceImportService {

    private static final int MAX_ROWS = 1000;
    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;
    private static final List<String> HEADERS = List.of(
            "公司名称", "纳税人识别号", "注册地址", "电话", "开户行", "银行账号"
    );

    private final InvoiceImportTaskMapper importTaskMapper;
    private final InvoiceTitleMapper invoiceTitleMapper;
    private final InvoiceTitleVersionMapper versionMapper;
    private final OperationLogMapper operationLogMapper;
    /** POI 依赖已包含 Jackson 2；这里使用私有实例，避免影响 Spring Boot 4 的 Jackson 3 全局配置。 */
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${invoice.import.storage-dir:../.runtime/data/imports}")
    private String storageDirectory;

    @Override
    public PageResult<ImportTaskVO> page(ImportTaskPageQueryDTO query) {
        long total = importTaskMapper.count(query);
        List<ImportTaskVO> records = total == 0
                ? Collections.emptyList()
                : importTaskMapper.selectPage(query).stream().map(this::toTaskVO).toList();
        return new PageResult<>(records, total, query.getPageNum(), query.getPageSize());
    }

    @Override
    public PageResult<ImportRowErrorVO> pageErrors(ImportRowErrorPageQueryDTO query) {
        long total = importTaskMapper.countRowErrors(query);
        List<ImportRowErrorVO> records = total == 0
                ? Collections.emptyList()
                : importTaskMapper.selectRowErrorPage(query).stream().map(this::toErrorVO).toList();
        return new PageResult<>(records, total, query.getPageNum(), query.getPageSize());
    }

    @Override
    @Transactional
    public ImportTaskVO importWorkbook(MultipartFile file, String operatorUserId, String operatorName) {
        validateFile(file);
        LocalDateTime now = LocalDateTime.now();
        String taskNo = createTaskNo(now);
        String safeFileName = sanitizeFileName(file.getOriginalFilename());
        Path storedFile = storeFile(file, taskNo, safeFileName);

        InvoiceImportTask task = new InvoiceImportTask();
        task.setTaskNo(taskNo);
        task.setOriginalFileName(safeFileName);
        task.setStorageProvider("LOCAL");
        task.setStorageKey(storedFile.toString());
        task.setStatus("VALIDATING");
        task.setTotalCount(0);
        task.setSuccessCount(0);
        task.setFailureCount(0);
        task.setStartedAt(now);
        task.setCreatedBy(operatorUserId);
        task.setCreatedAt(now);
        importTaskMapper.insert(task);

        int total = 0;
        int success = 0;
        int failure = 0;
        try (InputStream input = Files.newInputStream(storedFile); Workbook workbook = WorkbookFactory.create(input)) {
            Sheet sheet = requiredSheet(workbook);
            Map<String, Integer> columns = validateHeaders(sheet.getRow(0));
            DataFormatter formatter = new DataFormatter(Locale.SIMPLIFIED_CHINESE);
            Set<String> taxpayerIdsInFile = new HashSet<>();

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (isBlankRow(row, formatter)) {
                    continue;
                }
                total++;
                if (total > MAX_ROWS) {
                    throw new IllegalArgumentException("单次导入不能超过 " + MAX_ROWS + " 条数据");
                }
                Map<String, String> values = readRow(row, columns, formatter);
                RowValidation validation = validateRow(values, taxpayerIdsInFile);
                if (!validation.valid()) {
                    recordRowError(task.getId(), rowIndex + 1, values, validation.errorCode(), validation.message());
                    failure++;
                    continue;
                }
                createDraft(values, operatorUserId);
                taxpayerIdsInFile.add(values.get("纳税人识别号"));
                success++;
            }
        } catch (IllegalArgumentException exception) {
            // 文件级错误没有可定位的 Excel 数据行，任务保留为失败状态并通过日志说明原因。
            failure = Math.max(failure, Math.max(total - success, 0));
            finishTask(task, total, success, failure, "FAILED");
            writeOperationLog(task, operatorUserId, operatorName, "FAILED", exception.getMessage());
            return toTaskVO(task);
        } catch (IOException exception) {
            finishTask(task, total, success, Math.max(failure, 1), "FAILED");
            writeOperationLog(task, operatorUserId, operatorName, "FAILED", "Excel 文件读取失败");
            return toTaskVO(task);
        }

        String status = failure == 0 ? "COMPLETED" : success == 0 ? "FAILED" : "PARTIAL_FAILED";
        finishTask(task, total, success, failure, status);
        writeOperationLog(task, operatorUserId, operatorName, "FAILED".equals(status) ? "FAILED" : "SUCCESS",
                "总计 " + total + " 条，成功 " + success + " 条，失败 " + failure + " 条");
        return toTaskVO(task);
    }

    @Override
    public byte[] createTemplate() {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("发票抬头");
            Row header = sheet.createRow(0);
            for (int index = 0; index < HEADERS.size(); index++) {
                header.createCell(index).setCellValue(HEADERS.get(index));
                sheet.setColumnWidth(index, index == 2 ? 12000 : 6000);
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("生成导入模板失败", exception);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要导入的 Excel 文件");
        }
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new IllegalArgumentException("仅支持 .xlsx 格式文件");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Excel 文件不能超过 20 MB");
        }
    }

    private Path storeFile(MultipartFile file, String taskNo, String safeFileName) {
        try {
            Path directory = Path.of(storageDirectory).toAbsolutePath().normalize();
            Files.createDirectories(directory);
            Path target = directory.resolve(taskNo + "-" + safeFileName).normalize();
            if (!target.startsWith(directory)) {
                throw new IllegalArgumentException("文件名不合法");
            }
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, target);
            }
            return target;
        } catch (IOException exception) {
            throw new IllegalStateException("保存导入文件失败", exception);
        }
    }

    private Sheet requiredSheet(Workbook workbook) {
        if (workbook.getNumberOfSheets() == 0) {
            throw new IllegalArgumentException("Excel 中没有可导入的工作表");
        }
        return workbook.getSheetAt(0);
    }

    private Map<String, Integer> validateHeaders(Row headerRow) {
        if (headerRow == null) {
            throw new IllegalArgumentException("Excel 表头不能为空");
        }
        DataFormatter formatter = new DataFormatter(Locale.SIMPLIFIED_CHINESE);
        Map<String, Integer> columns = new LinkedHashMap<>();
        for (Cell cell : headerRow) {
            columns.put(formatter.formatCellValue(cell).trim(), cell.getColumnIndex());
        }
        List<String> missing = HEADERS.stream().filter(header -> !columns.containsKey(header)).toList();
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Excel 缺少表头：" + String.join("、", missing));
        }
        return columns;
    }

    private boolean isBlankRow(Row row, DataFormatter formatter) {
        if (row == null) {
            return true;
        }
        for (int index = 0; index < HEADERS.size(); index++) {
            Cell cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (cell != null && !formatter.formatCellValue(cell).trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private Map<String, String> readRow(Row row, Map<String, Integer> columns, DataFormatter formatter) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String header : HEADERS) {
            Cell cell = row.getCell(columns.get(header), Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            values.put(header, cell == null ? "" : formatter.formatCellValue(cell).trim());
        }
        return values;
    }

    private RowValidation validateRow(Map<String, String> values, Set<String> taxpayerIdsInFile) {
        if (values.get("公司名称").isBlank()) {
            return RowValidation.error("REQUIRED_MISSING", "公司名称不能为空");
        }
        String taxpayerId = values.get("纳税人识别号");
        if (taxpayerId.isBlank()) {
            return RowValidation.error("REQUIRED_MISSING", "纳税人识别号不能为空");
        }
        if (taxpayerIdsInFile.contains(taxpayerId) || invoiceTitleMapper.selectByTaxpayerId(taxpayerId) != null) {
            return RowValidation.error("DUPLICATE_TAXPAYER_ID", "纳税人识别号已存在或在当前文件中重复");
        }
        return RowValidation.success();
    }

    private void createDraft(Map<String, String> values, String operatorUserId) {
        LocalDateTime now = LocalDateTime.now();
        InvoiceTitle title = new InvoiceTitle();
        title.setCompanyName(values.get("公司名称"));
        title.setTaxpayerId(values.get("纳税人识别号"));
        title.setRegisteredAddress(blankToNull(values.get("注册地址")));
        title.setPhone(blankToNull(values.get("电话")));
        title.setBankName(blankToNull(values.get("开户行")));
        title.setBankAccount(blankToNull(values.get("银行账号")));
        title.setStatus(InvoiceTitleStatusEnum.DRAFT.getCode());
        title.setSubjectNames("");
        title.setCreatedBy(operatorUserId);
        title.setCreatedAt(now);
        title.setUpdatedBy(operatorUserId);
        title.setUpdatedAt(now);
        invoiceTitleMapper.insert(title);

        InvoiceTitleVersion version = new InvoiceTitleVersion();
        version.setTitleId(title.getId());
        version.setVersionNo(1);
        version.setStatus(InvoiceTitleStatusEnum.DRAFT.getCode());
        version.setChangeType("IMPORT");
        version.setChangeSummary("批量导入生成初始草稿");
        version.setCompanyName(title.getCompanyName());
        version.setTaxpayerId(title.getTaxpayerId());
        version.setRegisteredAddress(title.getRegisteredAddress());
        version.setPhone(title.getPhone());
        version.setBankName(title.getBankName());
        version.setBankAccount(title.getBankAccount());
        version.setSubjectIdsJson("[]");
        version.setCreatedBy(operatorUserId);
        version.setCreatedAt(now);
        versionMapper.insert(version);
    }

    private void recordRowError(Long taskId, int rowNo, Map<String, String> values, String code, String message) {
        InvoiceImportRowError error = new InvoiceImportRowError();
        error.setTaskId(taskId);
        error.setRowNo(rowNo);
        error.setTaxpayerId(blankToNull(values.get("纳税人识别号")));
        error.setErrorCode(code);
        error.setErrorMessage(message);
        error.setRawDataJson(toJson(values));
        error.setCreatedAt(LocalDateTime.now());
        importTaskMapper.insertRowError(error);
    }

    private void finishTask(InvoiceImportTask task, int total, int success, int failure, String status) {
        LocalDateTime finishedAt = LocalDateTime.now();
        importTaskMapper.updateResult(task.getId(), status, total, success, failure, finishedAt);
        task.setStatus(status);
        task.setTotalCount(total);
        task.setSuccessCount(success);
        task.setFailureCount(failure);
        task.setFinishedAt(finishedAt);
    }

    private void writeOperationLog(InvoiceImportTask task, String operatorUserId, String operatorName, String result, String detail) {
        OperationLog log = new OperationLog();
        log.setModuleType("IMPORT");
        log.setOperationType("IMPORT");
        log.setBusinessId(task.getTaskNo());
        log.setBusinessName(task.getOriginalFileName());
        log.setDetailJson(toJson(Map.of("message", detail, "status", task.getStatus())));
        log.setResult(result);
        log.setOperatorUserId(operatorUserId);
        log.setOperatorName(operatorName);
        log.setClientIp(null);
        log.setCreatedAt(LocalDateTime.now());
        operationLogMapper.insert(log);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("序列化导入数据失败", exception);
        }
    }

    private String sanitizeFileName(String originalFileName) {
        String fileName = originalFileName == null ? "invoice-title-import.xlsx" : Path.of(originalFileName).getFileName().toString();
        String sanitized = fileName.replaceAll("[^\\p{L}\\p{N}._-]", "_");
        return sanitized.isBlank() ? "invoice-title-import.xlsx" : sanitized;
    }

    private String createTaskNo(LocalDateTime now) {
        return "IMP" + now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private ImportTaskVO toTaskVO(InvoiceImportTask task) {
        ImportTaskVO vo = new ImportTaskVO();
        vo.setId(task.getId());
        vo.setTaskNo(task.getTaskNo());
        vo.setOriginalFileName(task.getOriginalFileName());
        vo.setStatus(task.getStatus());
        vo.setTotalCount(task.getTotalCount());
        vo.setSuccessCount(task.getSuccessCount());
        vo.setFailureCount(task.getFailureCount());
        vo.setStartedAt(task.getStartedAt());
        vo.setFinishedAt(task.getFinishedAt());
        vo.setCreatedBy(task.getCreatedBy());
        vo.setCreatedAt(task.getCreatedAt());
        return vo;
    }

    private ImportRowErrorVO toErrorVO(InvoiceImportRowError error) {
        ImportRowErrorVO vo = new ImportRowErrorVO();
        vo.setId(error.getId());
        vo.setRowNo(error.getRowNo());
        vo.setTaxpayerId(error.getTaxpayerId());
        vo.setErrorCode(error.getErrorCode());
        vo.setErrorMessage(error.getErrorMessage());
        vo.setRawDataJson(error.getRawDataJson());
        vo.setCreatedAt(error.getCreatedAt());
        return vo;
    }

    private record RowValidation(boolean valid, String errorCode, String message) {
        static RowValidation error(String code, String message) {
            return new RowValidation(false, code, message);
        }

        static RowValidation success() {
            return new RowValidation(true, null, null);
        }
    }
}
