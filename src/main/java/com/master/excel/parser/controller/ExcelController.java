package com.master.excel.parser.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.master.excel.parser.exception.FileConversionException;
import com.master.excel.parser.exception.InvalidExtension;
import com.master.excel.parser.service.ExcelService;
import com.master.excel.parser.utility.CsvToXlsxStreamingConverter;
import com.master.excel.parser.utility.ExcelConverter;
import java.io.ByteArrayOutputStream;

import com.master.excel.parser.utility.XlsxToCsvSaxConverter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class ExcelController {

    private final ExcelService excelService;
    private final XlsxToCsvSaxConverter xlsxToCsvSaxConverter;
    private final CsvToXlsxStreamingConverter csvToXlsxStreamingConverter;

    public ExcelController(ExcelService excelService, ExcelConverter excelConverter, XlsxToCsvSaxConverter xlsxToCsvSaxConverter, CsvToXlsxStreamingConverter csvToXlsxStreamingConverter) {
        this.excelService = excelService;
        this.xlsxToCsvSaxConverter = xlsxToCsvSaxConverter;
        this.csvToXlsxStreamingConverter = csvToXlsxStreamingConverter;
    }

    @PostMapping(value = "/upload", consumes = {"multipart/form-data"})
    public ResponseEntity<?> uploadExcels(
            @RequestParam("master") MultipartFile master,
            @RequestParam("live") MultipartFile live,
            @RequestParam("result") MultipartFile result,
            @RequestParam("mapping") String mappingJson,
            @RequestParam("details") String[] details,
            @RequestParam("vehicleType") String[] vehicleType,
            @RequestParam("vlookupMapping") String vlookupMapping,
            @RequestParam("dbParam") String[] dbParam,
            @RequestParam("fixedValues") String[] fixedValues
    ) {
        try {

            // Empty check for Master file
            if(master.isEmpty()){
                throw new InvalidExtension("Master file is not present!");
            }

            // Check if conversion is required or not for master file!
            if((Objects.requireNonNull(master.getOriginalFilename()).endsWith(".csv"))) {
                master = csvToXlsxStreamingConverter.convertToXlsx(master.getOriginalFilename(), master.getInputStream());
            }

            // Checks for result template and live file
            if (live.isEmpty()|| result.isEmpty()) {
                throw new InvalidExtension("Live or Result is empty or file extension is invalid, try using .csv extension!");
            }

            MultipartFile xlsxResult, xlsxLive;

            // Conversion of result template and live file (if required)
            try{
                if (Objects.requireNonNull(result.getOriginalFilename()).endsWith(".csv"))
                    xlsxResult = csvToXlsxStreamingConverter.convertToXlsx(result.getOriginalFilename(), result.getInputStream());
                else if(result.getOriginalFilename().endsWith(".xlsx"))
                    xlsxResult = result;
                else
                    throw new FileConversionException("Try using .xlsx or .csv for result file");

                if (Objects.requireNonNull(live.getOriginalFilename()).endsWith(".csv"))
                    xlsxLive = csvToXlsxStreamingConverter.convertToXlsx(live.getOriginalFilename(), live.getInputStream());
                else if(live.getOriginalFilename().endsWith(".xlsx"))
                    xlsxLive = live;
                else
                    throw new FileConversionException("Try using .xlsx or .csv for live file");
            } catch (Exception e) {
                throw new FileConversionException("Error occurred while converting files from .csv to .xlsx");
            }

            // Parsing mapping JSON string into a Map<String, String>
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> directMap = mapper.readValue(mappingJson, new TypeReference<Map<String, String>>() {});
            Map<String, String> vlookupMap = mapper.readValue(vlookupMapping, new TypeReference<LinkedHashMap<String, String>>() {});

            // Logging request info
            System.out.print("Details : ");
            for(String i : details) {
                System.out.print(i + ", ");
            }
            System.out.println();

            if(vehicleType.length == 2)
                System.out.println("Vehicle type: "+vehicleType[0]+", "+vehicleType[1]);
            System.out.println(mappingJson);
            System.out.println(vlookupMapping);

            // Calling the service method
            ByteArrayOutputStream outputStream = excelService.automateExcelPopulation(master, xlsxLive, xlsxResult, directMap, vlookupMap, details, vehicleType, dbParam, fixedValues);

            // Getting the original filename or using a default value
            String originalFileName = Optional.ofNullable(result.getOriginalFilename()).orElse("result_template.xlsx");

            // Timestamp for uniqueness
            String todayDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

            // Cleaning filename
            String safeFileName = "auto_generated_"+ details[4] + "_" + details[5] + "_" + todayDate + ".csv";

            // Preparing download response
            MultipartFile xlsxOutputFile = new MockMultipartFile(
                    "file",
                    "result.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    outputStream.toByteArray()
            );

            // Converting back from .xlsx to .csv
            MultipartFile csvOutputFile = xlsxToCsvSaxConverter.convertToCsv(xlsxOutputFile.getOriginalFilename(), xlsxOutputFile.getInputStream());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=" + safeFileName)
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(csvOutputFile.getBytes());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
