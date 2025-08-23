package com.master.excel.parser.utility;

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
     */
    public MultipartFile convertToXlsx(String originalFilename, InputStream csvInput) {
        File tempFile = null;
        try (
                BufferedReader br = new BufferedReader(new InputStreamReader(csvInput, StandardCharsets.UTF_8))
        ) {
            // ✅ Create temp file to hold XLSX instead of keeping in memory
            tempFile = File.createTempFile("csv-to-xlsx-", ".xlsx");

            try (FileOutputStream fos = new FileOutputStream(tempFile);
                 SXSSFWorkbook workbook = new SXSSFWorkbook(100)) { // keep 100 rows in memory

                Sheet sheet = workbook.createSheet("Sheet1");
                String line;
                int rowNum = 0;

                while ((line = br.readLine()) != null) {
                    Row row = sheet.createRow(rowNum++);
                    String[] cells = line.split(",", -1); // -1 keeps empty values
                    for (int i = 0; i < cells.length; i++) {
                        row.createCell(i).setCellValue(cells[i]);
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
