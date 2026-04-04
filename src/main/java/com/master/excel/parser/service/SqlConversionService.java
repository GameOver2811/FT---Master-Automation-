package com.master.excel.parser.service;

import com.master.excel.parser.exception.FileConversionException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class SqlConversionService {

    private static final int BATCH_SIZE = 100000;

    public ResponseEntity<?> getSqlFromCsv(MultipartFile file) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            Map<String, Integer> headerMap = csvParser.getHeaderMap();
            String[] headers = headerMap.keySet().toArray(new String[0]);

            String fileName = file.getOriginalFilename();
            if (fileName == null || !fileName.toLowerCase().endsWith(".csv")) {
                throw new IllegalArgumentException("Invalid CSV file");
            }

            String tableName = fileName.substring(0, fileName.lastIndexOf('.'));

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zos = new ZipOutputStream(baos);

            List<String> batchRows = new ArrayList<>();
            int rowCount = 0;
            int fileIndex = 1;

            for (CSVRecord record : csvParser) {
                List<String> values = new ArrayList<>();

                for (int i = 0; i < headers.length; i++) {
                    String raw = record.get(i);

                    if (raw == null || raw.trim().isEmpty() || raw.equalsIgnoreCase("null")) {
                        values.add("NULL"); // SQL NULL (no quotes)
                    } else {
                        String escaped = raw.replace("'", "''");
                        values.add("'" + escaped + "'");
                    }
                }


                batchRows.add("(" + String.join(", ", values) + ")");
                rowCount++;

                if (rowCount % BATCH_SIZE == 0) {
                    writeSqlFile(zos, tableName, headers, batchRows, fileIndex, fileIndex == 1);
                    batchRows.clear();
                    fileIndex++;
                }
            }

            if (!batchRows.isEmpty()) {
                writeSqlFile(zos, tableName, headers, batchRows, fileIndex, fileIndex == 1);
            }

            zos.close();

            byte[] zipBytes = baos.toByteArray();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + tableName + ".zip\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(zipBytes);

        } catch (IOException e) {
            throw new FileConversionException("Error processing CSV file: " + e.getMessage());
        }
    }

    private void writeSqlFile(
            ZipOutputStream zos,
            String tableName,
            String[] headers,
            List<String> rows,
            int fileIndex,
            boolean includeCreateTable
    ) throws IOException {

        StringBuilder sqlBuilder = new StringBuilder();

        if (includeCreateTable) {
            sqlBuilder.append("CREATE TABLE ").append(tableName).append(" (\n");
            sqlBuilder.append("    id INT AUTO_INCREMENT PRIMARY KEY,\n");

            for (int i = 0; i < headers.length; i++) {
                String cleanHeader = headers[i].trim().replaceAll("[^a-zA-Z0-9_]", "_");
                sqlBuilder.append("    ").append(cleanHeader).append(" VARCHAR(255)");
                if (i < headers.length - 1) sqlBuilder.append(",");
                sqlBuilder.append("\n");
            }
            sqlBuilder.append(");\n\n");
        }

        sqlBuilder.append("INSERT INTO ").append(tableName)
                .append(" (").append(String.join(", ", headers)).append(") VALUES\n");

        sqlBuilder.append(String.join(",\n", rows));
        sqlBuilder.append(";\n");

        ZipEntry entry = new ZipEntry(tableName + "_" + fileIndex + ".sql");
        zos.putNextEntry(entry);
        zos.write(sqlBuilder.toString().getBytes());
        zos.closeEntry();
    }
}
