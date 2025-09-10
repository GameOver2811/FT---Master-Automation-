package com.master.excel.parser.controller;

import com.master.excel.parser.utility.CsvToXlsxStreamingConverter;
import com.master.excel.parser.utility.ExcelConverter;
import com.master.excel.parser.utility.XlsxToCsvSaxConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/get")
public class AccessExcel {

    private final XlsxToCsvSaxConverter xlsxToCsvSaxConverter;
    private final CsvToXlsxStreamingConverter csvToXlsxStreamingConverter;

    public AccessExcel(XlsxToCsvSaxConverter xlsxToCsvSaxConverter, CsvToXlsxStreamingConverter csvToXlsxStreamingConverter) {
        this.xlsxToCsvSaxConverter = xlsxToCsvSaxConverter;
        this.csvToXlsxStreamingConverter = csvToXlsxStreamingConverter;
    }

    @PostMapping("/xlsx-csv")
    public ResponseEntity<?> getCSV(@RequestParam("xlsx") MultipartFile file) throws IOException {
        MultipartFile responseFile;
        try {
            responseFile = xlsxToCsvSaxConverter.convertToCsv(file.getOriginalFilename(), file.getInputStream());
        } catch (IOException e) {
            System.out.println("Nhi hue parse, kuch to locha hai!");
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=" + "xlsxToCsv.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(responseFile.getBytes());

    }

    @PostMapping("/csv-xlsx")
    public ResponseEntity<?> getXLSX(@RequestParam("csv") MultipartFile file) throws IOException {
        MultipartFile responseFile;
        try {
            responseFile = csvToXlsxStreamingConverter.convertToXlsx(file.getOriginalFilename(), file.getInputStream());
        } catch (IOException e) {
            System.out.println("Nhi hue parse, kuch to locha hai!");
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=csvToXlsx.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(responseFile.getBytes());

    }

}
