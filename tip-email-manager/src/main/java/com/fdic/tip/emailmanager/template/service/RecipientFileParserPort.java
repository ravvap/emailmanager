package com.fdic.tip.emailmanager.template.service;

import java.util.List;

/** Parses the uploaded recipient file's structure — sheet names and a sheet's header row. */
public interface RecipientFileParserPort {

    /** Worksheet names in the file, in order. A CSV file has exactly one, conventionally named "Sheet1". */
    List<String> listSheetNames(byte[] content, String fileName);

    /** Header row (first row) of the given sheet, in column order. */
    List<String> getColumnHeaders(byte[] content, String fileName, String sheetName);
}
