package com.fdic.tip.emailmanager.template.adapter;

import com.fdic.tip.emailmanager.common.constants.RecipientFileConstants;
import com.fdic.tip.emailmanager.template.service.RecipientFileParserPort;
import com.fdic.tip.emailmanager.template.service.RecipientFileRow;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Reads worksheet names and header rows out of the uploaded
 * FILE_UPLOAD-mode recipient file. .xlsx via Apache POI; .csv is
 * treated as a single implicit "Sheet1" and split on its header line.
 *
 * Maven: org.apache.poi:poi-ooxml
 */
@Component
public class RecipientFileParserPortImpl implements RecipientFileParserPort {

    private static final String CSV_SHEET_NAME = "Sheet1";

    @Override
    public List<String> listSheetNames(byte[] content, String fileName) {
        if (isCsv(fileName)) {
            return List.of(CSV_SHEET_NAME);
        }
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            List<String> names = new ArrayList<>();
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                names.add(workbook.getSheetName(i));
            }
            if (names.isEmpty()) {
                throw new IllegalStateException(RecipientFileConstants.MSG_NO_SHEETS_FOUND);
            }
            return names;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public List<String> getColumnHeaders(byte[] content, String fileName, String sheetName) {
        if (isCsv(fileName)) {
            return parseCsvHeader(content);
        }
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new IllegalArgumentException(RecipientFileConstants.MSG_SHEET_NOT_FOUND);
            }
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                throw new IllegalStateException(RecipientFileConstants.MSG_EMPTY_SHEET);
            }
            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(cell.getStringCellValue());
            }
            return headers;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public List<RecipientFileRow> getRecipientRows(byte[] content, String fileName, String sheetName,
                                                     String emailColumn, String nameColumn) {
        if (isCsv(fileName)) {
            return parseCsvRows(content, emailColumn, nameColumn);
        }
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new IllegalArgumentException(RecipientFileConstants.MSG_SHEET_NOT_FOUND);
            }
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                throw new IllegalStateException(RecipientFileConstants.MSG_EMPTY_SHEET);
            }
            int emailColIndex = findColumnIndex(headerRow, emailColumn);
            int nameColIndex = findColumnIndex(headerRow, nameColumn);

            List<RecipientFileRow> rows = new ArrayList<>();
            int rowNumber = 0;
            for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row dataRow = sheet.getRow(r);
                if (dataRow == null || isBlankRow(dataRow)) {
                    continue;
                }
                rowNumber++;
                String email = emailColIndex >= 0 ? getCellText(dataRow.getCell(emailColIndex)) : null;
                String name = nameColIndex >= 0 ? getCellText(dataRow.getCell(nameColIndex)) : null;
                rows.add(new RecipientFileRow(rowNumber, name, email));
            }
            return rows;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private int findColumnIndex(Row headerRow, String columnName) {
        if (columnName == null || columnName.isBlank()) {
            return -1;
        }
        for (Cell cell : headerRow) {
            if (columnName.equals(cell.getStringCellValue())) {
                return cell.getColumnIndex();
            }
        }
        return -1;
    }

    private boolean isBlankRow(Row row) {
        for (Cell cell : row) {
            if (cell.getCellType() != CellType.BLANK && !getCellText(cell).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String getCellText(Cell cell) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    private List<RecipientFileRow> parseCsvRows(byte[] content, String emailColumn, String nameColumn) {
        String text = new String(content, StandardCharsets.UTF_8);
        String[] lines = text.split("\\r?\\n");
        if (lines.length == 0 || lines[0].isBlank()) {
            throw new IllegalStateException(RecipientFileConstants.MSG_EMPTY_SHEET);
        }
        List<String> headers = new ArrayList<>();
        for (String column : lines[0].split(",")) {
            headers.add(column.trim());
        }
        int emailColIndex = headers.indexOf(emailColumn);
        int nameColIndex = headers.indexOf(nameColumn);

        List<RecipientFileRow> rows = new ArrayList<>();
        int rowNumber = 0;
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isBlank()) {
                continue;
            }
            rowNumber++;
            String[] values = lines[i].split(",", -1);
            String email = (emailColIndex >= 0 && emailColIndex < values.length) ? values[emailColIndex].trim() : null;
            String name = (nameColIndex >= 0 && nameColIndex < values.length) ? values[nameColIndex].trim() : null;
            rows.add(new RecipientFileRow(rowNumber, name, email));
        }
        return rows;
    }

    private boolean isCsv(String fileName) {
        return fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".csv");
    }

    private List<String> parseCsvHeader(byte[] content) {
        String text = new String(content, StandardCharsets.UTF_8);
        int newlineIndex = text.indexOf('\n');
        String headerLine = (newlineIndex == -1 ? text : text.substring(0, newlineIndex)).replace("\r", "");
        if (headerLine.isBlank()) {
            throw new IllegalStateException(RecipientFileConstants.MSG_EMPTY_SHEET);
        }
        List<String> headers = new ArrayList<>();
        for (String column : headerLine.split(",")) {
            headers.add(column.trim());
        }
        return headers;
    }
}
