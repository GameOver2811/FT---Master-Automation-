package com.master.excel.parser.service;

import com.master.excel.parser.dao.VehicleMapping2WDao;
import com.master.excel.parser.dao.VehicleMapping4WDao;
import com.master.excel.parser.dao.VehicleMappingCVDao;
import com.master.excel.parser.dto.VehicleMapping2W;
import com.master.excel.parser.dto.VehicleMapping4W;
import com.master.excel.parser.dto.VehicleMappingCV;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.util.XMLHelper;
import org.apache.poi.xssf.eventusermodel.*;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@Transactional
public class ExcelService {

    @Autowired
    VehicleMappingCVDao vehicleMappingCVDao;

    @Autowired
    VehicleMapping2WDao vehicleMapping2WDao;

    @Autowired
    VehicleMapping4WDao vehicleMapping4WDao;

    public ByteArrayOutputStream automateExcelPopulation(
            MultipartFile masterFile,
            MultipartFile liveFile,
            MultipartFile resultTemplateFile,
            Map<String, String> directMap,
            Map<String, String> vlookupMap,
            String[] details,
            String[] vehicleType,
            String[] dbParam) throws Exception {

        // ----------------------------
        // Step 1: Load result template (small file)
        // ----------------------------
        Workbook resultWb = new XSSFWorkbook(resultTemplateFile.getInputStream());
        Sheet resultSheet = resultWb.getSheetAt(0);

        Map<String, Integer> resultIndexes = getColumnIndexes(resultSheet.getRow(0));

        // ----------------------------
        // Step 2: Parse masterFile with SAX
        // ----------------------------
        List<Map<String, String>> masterRows = new ArrayList<>();

        File tempMaster = File.createTempFile("master-", ".xlsx");
        masterFile.transferTo(tempMaster);

        try (OPCPackage pkg = OPCPackage.open(tempMaster, PackageAccess.READ)) {
            XSSFReader reader = new XSSFReader(pkg);
            StylesTable styles = reader.getStylesTable();
            ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(pkg);

            XMLReader parser = XMLHelper.newXMLReader();
            parser.setContentHandler(new XSSFSheetXMLHandler(styles, strings, new XSSFSheetXMLHandler.SheetContentsHandler() {
                private Map<Integer, String> currentRow = new HashMap<>();
                private List<String> headers = new ArrayList<>();
                private int currentCol = -1;

                @Override
                public void startRow(int rowNum) {
                    currentRow.clear();
                    currentCol = -1;
                }

                @Override
                public void endRow(int rowNum) {
                    if (rowNum == 0) {
                        TreeMap<Integer, String> sorted = new TreeMap<>(currentRow);
                        headers.clear();
                        sorted.values().forEach(h -> headers.add(h.trim().toLowerCase()));
                    } else {
                        TreeMap<Integer, String> sorted = new TreeMap<>(currentRow);
                        Map<String, String> row = new LinkedHashMap<>();
                        for (Map.Entry<Integer, String> entry : sorted.entrySet()) {
                            int colIndex = entry.getKey();
                            if (colIndex < headers.size()) {
                                row.put(headers.get(colIndex), entry.getValue());
                            }
                        }
                        boolean hasData = row.values().stream().anyMatch(v -> v != null && !v.trim().isEmpty());
                        if (hasData) masterRows.add(row);
                    }
                }

                @Override
                public void cell(String cellReference, String formattedValue, org.apache.poi.xssf.usermodel.XSSFComment comment) {
                    int colIndex;
                    if (cellReference != null) {
                        colIndex = getColumnIndex(cellReference);
                        currentCol = colIndex;
                    } else {
                        colIndex = ++currentCol;
                    }
                    currentRow.put(colIndex, formattedValue);
                }

                @Override
                public void headerFooter(String text, boolean isHeader, String tagName) {}
            }, false));

            try (InputStream sheet = reader.getSheetsData().next()) {
                parser.parse(new InputSource(sheet));
            }
        } finally {
            tempMaster.delete();
        }

        // ----------------------------
        // Step 3: Parse liveFile with SAX (build vlookup maps)
        // ----------------------------
        Map<String, Map<String, String>> vlookupMappingArrMap = new HashMap<>();

        File tempLive = File.createTempFile("live-", ".xlsx");
        liveFile.transferTo(tempLive);

        try (OPCPackage pkg = OPCPackage.open(tempLive, PackageAccess.READ)) {
            XSSFReader reader = new XSSFReader(pkg);
            StylesTable styles = reader.getStylesTable();
            ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(pkg);

            // Build header + lookup maps
            Map<Integer, String> headerMap = new HashMap<>();

            XMLReader parser = XMLHelper.newXMLReader();
            parser.setContentHandler(new XSSFSheetXMLHandler(styles, strings, new XSSFSheetXMLHandler.SheetContentsHandler() {
                private Map<Integer, String> currentRow = new HashMap<>();
                private int currentCol = -1;

                @Override
                public void startRow(int rowNum) {
                    currentRow.clear();
                    currentCol = -1;
                }

                @Override
                public void endRow(int rowNum) {
                    if (rowNum == 0) {
                        for (Map.Entry<Integer, String> e : currentRow.entrySet()) {
                            headerMap.put(e.getKey(), e.getValue().trim().toLowerCase());
                        }
                    } else {
                        for (Map.Entry<String, String> entry : vlookupMap.entrySet()) {
                            String rewardHeader = entry.getKey().trim().toLowerCase();
                            String modelHeader = entry.getValue().trim().toLowerCase();

                            Integer rewardIdx = findHeaderIndex(headerMap, rewardHeader);
                            Integer modelIdx = findHeaderIndex(headerMap, modelHeader);

                            if (rewardIdx == null || modelIdx == null) continue;

                            String rewardType = currentRow.getOrDefault(rewardIdx, "").trim();
                            String modelCode = currentRow.getOrDefault(modelIdx, "").trim();

                            if (!rewardType.isEmpty() && !modelCode.isEmpty()) {
                                vlookupMappingArrMap
                                        .computeIfAbsent(rewardHeader, k -> new HashMap<>())
                                        .put(modelCode, rewardType);
                            }
                        }
                    }
                }

                @Override
                public void cell(String cellReference, String formattedValue, org.apache.poi.xssf.usermodel.XSSFComment comment) {
                    int colIndex;
                    if (cellReference != null) {
                        colIndex = getColumnIndex(cellReference);
                        currentCol = colIndex;
                    } else {
                        colIndex = ++currentCol;
                    }
                    currentRow.put(colIndex, formattedValue);
                }

                @Override
                public void headerFooter(String text, boolean isHeader, String tagName) {}
            }, false));

            try (InputStream sheet = reader.getSheetsData().next()) {
                parser.parse(new InputSource(sheet));
            }
        } finally {
            tempLive.delete();
        }

        // ----------------------------
        // Step 4: Process rows & fill result sheet
        // ----------------------------
        int rowNum = 1;
        for (Map<String, String> masterRow : masterRows) {

            if(vehicleType.length > 0) {
                String vehicleTypeKey = vehicleType[0].trim().toLowerCase();
                if (!masterRow.containsKey(vehicleTypeKey)) continue;
                boolean hasMatchingType = false;
                for (int i = 1; i < vehicleType.length; i++) {
                    String candidateType = vehicleType[i].trim();
                    if (masterRow.get(vehicleTypeKey).equalsIgnoreCase(candidateType)) {
                        hasMatchingType = true;
                        break;
                    }
                }
                if (!hasMatchingType) continue;
            }

            Row resultRow = resultSheet.createRow(rowNum++);

            if (resultIndexes.containsKey("id")) {
                resultRow.createCell(resultIndexes.get("id")).setCellValue("NULL");
            }

            // Direct mappings
            for (Map.Entry<String, String> mapping : directMap.entrySet()) {
                String resultCol = mapping.getKey().toLowerCase();
                String masterCol = mapping.getValue().toLowerCase();
                int resultColIndex = resultIndexes.getOrDefault(resultCol, -1);

                if (resultColIndex != -1) {
                    String value = masterRow.getOrDefault(masterCol, "");
                    resultRow.createCell(resultColIndex).setCellValue(value);
                }
            }

            // Fuel type normalization
            if (resultIndexes.containsKey(details[0])) {
                String fuelType = masterRow.getOrDefault(details[1].toLowerCase(), "").trim();
                String fuelMapped = mapFuelType(fuelType);
                resultRow.createCell(resultIndexes.get(details[0])).setCellValue(fuelMapped);
            }

            // vlookup mappings
            for (Map.Entry<String, String> e : vlookupMap.entrySet()) {
                String rewardHeader = e.getKey().toLowerCase();
                String modelHeader = e.getValue().toLowerCase();

                Map<String, String> currentMapping = vlookupMappingArrMap.get(rewardHeader);
                if (currentMapping == null) continue;

                if (resultIndexes.containsKey(rewardHeader) && resultIndexes.containsKey(modelHeader)) {
                    Cell modelCodeCell = resultRow.getCell(resultIndexes.get(modelHeader));
                    String modelCode = getCellValueAsString(modelCodeCell).trim();
                    String valueFromMap = currentMapping.getOrDefault(modelCode, "#N/A");

                    String safeValue = ("reward_vehicle_type".equalsIgnoreCase(rewardHeader))
                            ? handleRewardVehicleType(resultRow, resultIndexes, dbParam, modelCode, valueFromMap, details)
                            : valueFromMap;

                    resultRow.createCell(resultIndexes.get(rewardHeader)).setCellValue(safeValue);
                }
            }

            // Electric CC → Power
            if (resultIndexes.containsKey(details[2])) {
                Cell ccCell = resultRow.getCell(resultIndexes.getOrDefault(details[3].toLowerCase(), 0));
                Cell fuelTypeCell = resultRow.getCell(resultIndexes.getOrDefault(details[0], 0));
                String ccStr = getCellValueAsString(ccCell).trim();
                String fuelType = getCellValueAsString(fuelTypeCell).trim();

                if ("Electric".equalsIgnoreCase(fuelType) && !ccStr.isEmpty()) {
                    try {
                        float ccValue = Float.parseFloat(ccStr);
                        float rawCC = ccValue < 1000 ? ccValue : ccValue / 1000f;
                        BigDecimal bd = new BigDecimal(rawCC).setScale(1, RoundingMode.HALF_UP);
                        resultRow.createCell(resultIndexes.get(details[2])).setCellValue(bd.toPlainString());
                    } catch (NumberFormatException ex) {
                        resultRow.createCell(resultIndexes.get(details[2])).setCellValue(ccStr);
                    }
                }
            }
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        resultWb.write(out);
        resultWb.close();
        return out;
    }

    // --- Helpers ---

    private Map<String, Integer> getColumnIndexes(Row headerRow) {
        Map<String, Integer> map = new HashMap<>();
        if (headerRow == null) return map;
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null) {
                map.put(cell.getStringCellValue().trim().toLowerCase(), i);
            }
        }
        return map;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    private int getColumnIndex(String cellRef) {
        int col = 0;
        for (int i = 0; i < cellRef.length(); i++) {
            if (Character.isDigit(cellRef.charAt(i))) break;
            col = col * 26 + (cellRef.charAt(i) - 'A' + 1);
        }
        return col - 1;
    }

    private Integer findHeaderIndex(Map<Integer, String> headers, String name) {
        return headers.entrySet().stream()
                .filter(e -> e.getValue().equalsIgnoreCase(name))
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);
    }

    private String mapFuelType(String fuelType) {
        return switch (fuelType.toUpperCase()) {
            case "PETROL", "PETROL WITH CNG", "PETROL+CNG", "PETROL T", "PETROL C", "PH", "PETROL(P)", "PETROL HYBRID(PH)", "PETROL P", "PETROL G", "PETROL+LPG", "P" -> "Petrol";
            case "DIESEL", "DIESEL T", "DIESEL C", "DH", "DIESEL HYBRID(DH)", "DIESEL(D)", "DIESEL P", "DIESEL G", "D" -> "Diesel";
            case "CNG", "CH", "CNG(C)", "C" -> "CNG";
            case "LPG" -> "LPG";
            case "LNG" -> "LNG";
            case "ELECTRIC", "ELECTRICAL", "BATTERY", "ELECTRICITY", "ELECTRIC T", "ELECTRIC C", "ELECTRIC HYBRID", "BATTERY(B)", "BATTERY OPERATED", "ELECTRIC P", "B" -> "Electric";
            case "HYBRID", "HYBRID(H)", "MILD HYBRID", "PLUG IN HYBRID", "HYBRID ELECTRIC VEHICLE" -> "Hybrid";
            default -> "";
        };
    }

    private String handleRewardVehicleType(Row resultRow, Map<String, Integer> resultIndexes, String[] dbParam,
                                           String modelCode, String valueFromMap, String[] details) {

        StringBuilder sb = new StringBuilder();
        for (String key : dbParam) {
            int colIndex = resultIndexes.getOrDefault(key.toLowerCase(), -1);
            if (colIndex != -1) {
                Cell cell = resultRow.getCell(colIndex);
                String value = getCellValueAsString(cell).replaceAll("\\s+", "");
                sb.append(value);
            }
        }
        String concatenatedString = sb.toString().toLowerCase();

        switch (details[4].trim().toUpperCase()) {
            case "2W" -> valueFromMap = handle2wData(concatenatedString, valueFromMap, details[4], details[5]);
            case "4W" -> valueFromMap = handle4wData(concatenatedString, valueFromMap, details[4], details[5]);
            case "CV", "PCV", "GCV", "MISD" -> valueFromMap = handleCvData(concatenatedString, valueFromMap, details[4], details[5]);
        }
        return ("NULL".equals(valueFromMap) || "#N/A".equals(valueFromMap)) ? "#N/A" : valueFromMap;
    }

    private String handleCvData(String concatenatedString, String valueFromMap, String product, String ic) {
        boolean contains = vehicleMappingCVDao.containsVehicleModelString(concatenatedString, ic);
        if("NULL".equals(valueFromMap) || "#N/A".equals(valueFromMap)) {
            if(contains) {
                return vehicleMappingCVDao.getRewardVehicleType(concatenatedString, ic);
            }
        } else {
            if(!contains) {
                VehicleMappingCV data = new VehicleMappingCV(concatenatedString, valueFromMap, product, ic);
                vehicleMappingCVDao.save(data, ic);
            }
        }
        return valueFromMap;
    }

    private String handle4wData(String concatenatedString, String valueFromMap, String product, String ic) {
        Boolean contains = vehicleMapping4WDao.containsVehicleModelString(concatenatedString);

        if (("NULL".equals(valueFromMap) || "#N/A".equals(valueFromMap)) && contains) {
            return vehicleMapping4WDao.getRewardVehicleType(concatenatedString);
        }

        if (!"NULL".equals(valueFromMap) && !"#N/A".equals(valueFromMap) && !contains) {
            VehicleMapping4W data = new VehicleMapping4W(concatenatedString, valueFromMap, product, ic);
            vehicleMapping4WDao.save(data);
        }

        return valueFromMap;

    }

    private String handle2wData(String concatenatedString, String valueFromMap, String product, String ic) {
        Boolean contains = vehicleMapping2WDao.containsVehicleModelString(concatenatedString);

        System.out.println("Concat : " + concatenatedString + ", valueFromMap : " + valueFromMap + ", product : " + product + ", IC : " + ic);

        if (("NULL".equals(valueFromMap) || "#N/A".equals(valueFromMap)) && contains) {
            return vehicleMapping2WDao.getRewardVehicleType(concatenatedString);
        }

        if (!"NULL".equals(valueFromMap) && !"#N/A".equals(valueFromMap) && !contains) {
            VehicleMapping2W data = new VehicleMapping2W(concatenatedString, valueFromMap, product, ic);
            vehicleMapping2WDao.save(data);
        }

        return valueFromMap;

    }

}
