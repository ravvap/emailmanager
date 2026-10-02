package com.fdic.tip.emailmanager.template.adapter;

import com.fdic.tip.emailmanager.common.constants.RecipientFileConstants;
import com.fdic.tip.emailmanager.template.service.RecipientFileParserPort;
import org.apache.poi.ss.usermodel.Cell;
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
