package com.master.excel.parser.service;

import com.master.excel.parser.exception.FileConversionException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SqlConversionService {
    public ResponseEntity<?> getSqlFromCsv(MultipartFile file) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            Map<String, Integer> headerMap = csvParser.getHeaderMap();
            String[] headers = headerMap.keySet().toArray(new String[0]);
            String tableName = "auto_generated";

            StringBuilder sqlBuilder = new StringBuilder();

            // CREATE TABLE statement with auto-increment ID
            sqlBuilder.append("CREATE TABLE ").append(tableName).append(" (\n");
            sqlBuilder.append("    id INT AUTO_INCREMENT PRIMARY KEY,\n"); // auto-increment ID
            for (int i = 0; i < headers.length; i++) {
                String cleanHeader = headers[i].trim().replaceAll("[^a-zA-Z0-9_]", "_");
                sqlBuilder.append("    ").append(cleanHeader).append(" VARCHAR(255)");
                if (i < headers.length - 1) sqlBuilder.append(",");
                sqlBuilder.append("\n");
            }
            sqlBuilder.append(");\n\n");

            // Multi-row INSERT (exclude the ID column, it will auto-generate)
            sqlBuilder.append("INSERT INTO ").append(tableName).append(" (");
            sqlBuilder.append(String.join(", ", headers));
            sqlBuilder.append(") VALUES\n");

            List<String> rows = new ArrayList<>();
            for (CSVRecord record : csvParser) {
                List<String> values = new ArrayList<>();
                for (int i = 0; i < headers.length; i++) {
                    String value = record.get(i).replace("'", "''");
                    if (headers[i].trim().equalsIgnoreCase("id"))
                        values.add(value);
                    else
                        values.add("'" + value + "'");
                }
                rows.add("(" + String.join(", ", values) + ")");
            }

            sqlBuilder.append(String.join(",\n", rows));
            sqlBuilder.append(";\n");

            byte[] sqlBytes = sqlBuilder.toString().getBytes();
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + tableName + ".sql\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(sqlBytes);

        } catch (IOException e) {
            throw new FileConversionException("Error processing CSV file: " + e.getMessage());
        }
    }
}
