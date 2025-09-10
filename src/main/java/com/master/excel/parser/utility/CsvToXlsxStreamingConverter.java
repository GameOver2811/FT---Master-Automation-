package com.master.excel.parser.utility;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;

@Component
public class CsvToXlsxStreamingConverter {

    /**
     * Converts CSV to XLSX efficiently using SXSSFWorkbook (streaming, no OOM).
     * Handles quoted fields and commas inside values using Apache Commons CSV.
     */
    public MultipartFile convertToXlsx(String originalFilename, InputStream csvInput) {
        File tempFile = null;
        try (
                Reader reader = new InputStreamReader(csvInput, StandardCharsets.UTF_8)
        ) {
            // ✅ Create temp file to hold XLSX instead of keeping in memory
            tempFile = File.createTempFile("csv-to-xlsx-", ".xlsx");

            try (FileOutputStream fos = new FileOutputStream(tempFile);
                 SXSSFWorkbook workbook = new SXSSFWorkbook(100)) { // keep 100 rows in memory

                Sheet sheet = workbook.createSheet("Sheet1");

                Iterable<CSVRecord> records = CSVFormat.DEFAULT
                        .withTrim()
                        .parse(reader);

                int rowNum = 0;
                for (CSVRecord record : records) {
                    Row row = sheet.createRow(rowNum++);
                    for (int i = 0; i < record.size(); i++) {
                        row.createCell(i).setCellValue(record.get(i));
                    }
                }

                workbook.write(fos);
                workbook.dispose(); // cleanup SXSSF temp rows
            }

            // ✅ Build MultipartFile from temp file
            String xlsxFilename = originalFilename.replaceAll("\\.csv$", "") + ".xlsx";
            return new MockMultipartFile(
                    "file",
                    xlsxFilename,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    new FileInputStream(tempFile)
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to convert CSV to XLSX: " + e.getMessage(), e);
        } finally {
            if (tempFile != null && tempFile.exists()) {
                // optional: delete temp file on exit
                tempFile.deleteOnExit();
            }
        }
    }
}
