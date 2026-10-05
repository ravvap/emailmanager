package com.fdic.tip.emailmanager.template.service;

import java.util.List;

/** Parses the uploaded recipient file's structure — sheet names, a sheet's header row, and its data rows. */
public interface RecipientFileParserPort {

    /** Worksheet names in the file, in order. A CSV file has exactly one, conventionally named "Sheet1". */
    List<String> listSheetNames(byte[] content, String fileName);

    /** Header row (first row) of the given sheet, in column order. */
    List<String> getColumnHeaders(byte[] content, String fileName, String sheetName);

    /**
     * Every data row (excluding the header) of the given sheet, with just
     * the two mapped columns' raw values pulled out. rowNumber is 1-based,
     * counted from the first data row (header excluded).
     */
    List<RecipientFileRow> getRecipientRows(byte[] content, String fileName, String sheetName,
                                             String emailColumn, String nameColumn);
}
