package com.master.excel.parser.controller;

import com.master.excel.parser.exception.FileConversionException;
import com.master.excel.parser.service.SqlConversionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.util.*;

@RestController
@RequestMapping("/api/get")
public class SqlConversionController {

    @Autowired
    SqlConversionService sqlConversionService;

    @PostMapping("/csv-to-sql")
    public ResponseEntity<?> getSqlFromCsv(@RequestParam("csv") MultipartFile file) {
        if (file.isEmpty() || !Objects.requireNonNull(file.getOriginalFilename()).endsWith(".csv")) {
            throw new FileConversionException("Kindly try uploading .csv file with some data!");
        }
        return sqlConversionService.getSqlFromCsv(file);
    }
}
