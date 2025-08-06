package com.master.excel.parser.utility;

import com.master.excel.parser.exception.FileConversionException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockMultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class ExcelConverter {

    public byte[] convertXlsxToCsv(byte[] excelBytes) throws IOException {
        try (
                ByteArrayInputStream bais = new ByteArrayInputStream(excelBytes);
                XSSFWorkbook workbook = new XSSFWorkbook(bais);
                ByteArrayOutputStream csvOut = new ByteArrayOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(csvOut))
        ) {
            XSSFSheet sheet = workbook.getSheetAt(0); // Or loop all sheets

            for (Row row : sheet) {
                List<String> cells = new ArrayList<>();
                for (Cell cell : row) {
                    switch (cell.getCellType()) {
                        case STRING:
                            cells.add(cell.getStringCellValue());
                            break;
                        case NUMERIC:
                            cells.add(String.valueOf(cell.getNumericCellValue()));
                            break;
                        case BOOLEAN:
                            cells.add(String.valueOf(cell.getBooleanCellValue()));
                            break;
                        case FORMULA:
                            cells.add(cell.getCellFormula());
                            break;
                        case BLANK:
                            cells.add("");
                            break;
                        default:
                            cells.add("");
                    }
                }
                writer.write(String.join(",", cells));
                writer.newLine();
            }

            writer.flush();
            return csvOut.toByteArray();
        }
    }

    public MultipartFile csvToXlsx(MultipartFile csvFile) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(csvFile.getInputStream(), StandardCharsets.UTF_8));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Sheet1");
            String line;
            int rowNum = 0;

            while ((line = br.readLine()) != null) {
                Row row = sheet.createRow(rowNum++);
                String[] cells = line.split(",", -1); // handling empty values
                for (int i = 0; i < cells.length; i++) {
                    row.createCell(i).setCellValue(cells[i]);
                }
            }

            workbook.write(out);
            workbook.close();

            return new MockMultipartFile(
                    csvFile.getName(),
                    csvFile.getOriginalFilename().replace(".csv", ".xlsx"),
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    new ByteArrayInputStream(out.toByteArray())
            );

        } catch (Exception e) {
            throw new FileConversionException("Failed to convert CSV to XLSX: " + e.getMessage());
        }
    }

    public MultipartFile xlsxToCsv(MultipartFile xlsxFile) {
        try (Workbook workbook = new XSSFWorkbook(xlsxFile.getInputStream());
             ByteArrayOutputStream out = new ByteArrayOutputStream();
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {

            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter(); // handles date/numeric formatting correctly

            for (Row row : sheet) {
                StringBuilder rowBuilder = new StringBuilder();

                int lastColumn = row.getLastCellNum();
                for (int i = 0; i < lastColumn; i++) {
                    Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    String cellValue = (cell == null) ? "" : formatter.formatCellValue(cell);

                    // Escape cell value for CSV
                    rowBuilder.append(escapeCsv(cellValue));

                    if (i < lastColumn - 1) {
                        rowBuilder.append(",");
                    }
                }

                writer.write(rowBuilder.toString());
                writer.newLine();
            }

            writer.flush();

            return new MockMultipartFile(
                    xlsxFile.getName(),
                    xlsxFile.getOriginalFilename().replace(".xlsx", ".csv"),
                    "text/csv",
                    new ByteArrayInputStream(out.toByteArray())
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to convert XLSX to CSV: " + e.getMessage(), e);
        }
    }

    // Escapes CSV values with quotes and special characters
    private String escapeCsv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            value = value.replace("\"", "\"\""); // escape double quotes
            return "\"" + value + "\"";
        }
        return value;
    }


    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
            default:
                return "";
        }
    }

}
