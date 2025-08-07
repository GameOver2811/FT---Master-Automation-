package com.master.excel.parser.service;

import com.master.excel.parser.dao.VehicleMappingDao;
import com.master.excel.parser.dto.Mapping;
import com.master.excel.parser.exception.CubicCapacityException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.util.XMLHelper;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler.SheetContentsHandler;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class ExcelService {

    @Autowired
    VehicleMappingDao vehicleMappingDao;

    public ByteArrayOutputStream automateExcelPopulation(
            MultipartFile masterFile,
            MultipartFile liveFile,
            MultipartFile resultTemplateFile,
            Map<String, String> directMap,
            Map<String, String> vlookupMap,
            String[] details,
            String[] vehicleType,
            String[] dbParam) throws Exception {

        HashSet<String> validRewardVehicleType = new HashSet<>(
                Arrays.asList("NONHEV", "HEV", "Bike", "Scooter")
        );

        Workbook liveWb = new XSSFWorkbook(liveFile.getInputStream());
        liveWb.setSheetName(0, "Live");

        Workbook resultWb = new XSSFWorkbook(resultTemplateFile.getInputStream());
        Sheet liveSheet = liveWb.getSheetAt(0);
        Sheet resultSheet = resultWb.getSheetAt(0);

        Map<String, Integer> resultIndexes = getColumnIndexes(resultSheet.getRow(0));
        Map<String, Integer> liveIndexes = getColumnIndexes(liveSheet.getRow(0));

        List<Map<String, String>> masterRows = new ArrayList<>();

        try (OPCPackage pkg = OPCPackage.open(masterFile.getInputStream())) {
            XSSFReader reader = new XSSFReader(pkg);
            StylesTable styles = reader.getStylesTable();
            ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(pkg);

            XMLReader parser = XMLHelper.newXMLReader();
            parser.setContentHandler(new XSSFSheetXMLHandler(styles, strings, new SheetContentsHandler() {
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
                        sorted.values().forEach(h -> headers.add(h.trim().toLowerCase())); // Normalizing master header
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
                        if (hasData) {
                            masterRows.add(row);
                            // System.out.println(row);
                        }
                    }
                }

                @Override
                public void cell(String cellReference, String formattedValue, org.apache.poi.xssf.usermodel.XSSFComment comment) {
//                    int colIndex = getColumnIndex(cellReference);
//                    currentRow.put(colIndex, formattedValue);
                    int colIndex;
                    if (cellReference != null) {
                        colIndex = getColumnIndex(cellReference);
                        currentCol = colIndex;
                    } else {
                        colIndex = ++currentCol; // guess next column index
                    }

                    if (cellReference == null) {
                        System.out.println("Warning: cellReference is null, guessing column index as " + currentCol);
                    }

                    currentRow.put(colIndex, formattedValue);
                }

                @Override
                public void headerFooter(String text, boolean isHeader, String tagName) {
                }
            }, false));

            InputStream sheet = reader.getSheetsData().next();
            parser.parse(new InputSource(sheet));
        }

        // Normalizing directMap
        Map<String, String> normalizedDirectMap = new HashMap<>();
        for (Map.Entry<String, String> e : directMap.entrySet()) {
            normalizedDirectMap.put(e.getKey().toLowerCase(), e.getValue().toLowerCase());
        }

        // Normalizing vlookupMap
        Map<String, String> normalizedVlookupMap = new LinkedHashMap<>();
        for(Map.Entry<String, String> e : vlookupMap.entrySet()) {
            normalizedVlookupMap.put(e.getKey().toLowerCase(), e.getValue().toLowerCase());
        }

        System.out.println("Normalized Mapping: "+ normalizedDirectMap);
        System.out.println("Normalized Vlookup Mapping: "+ normalizedVlookupMap);

        // Ye loop change hua hai
        // Building lookup maps from live file
        ArrayList<Map<String, String>> vlookupMappingArr = new ArrayList<>();
        for(Map.Entry<String, String> e : vlookupMap.entrySet()) {

            Map<String, String> columnMapping = new HashMap<>();

            for (int i = 1; i <= liveSheet.getLastRowNum(); i++) {
                Row liveRow = liveSheet.getRow(i);

                //System.out.println(liveRow);

                if (liveRow == null) continue;

                String modelCodeHeader = e.getValue().trim().toLowerCase();
                String rewardTypeHeader = e.getKey().trim().toLowerCase();

                if (!liveIndexes.containsKey(modelCodeHeader) || !liveIndexes.containsKey(rewardTypeHeader)) {
                    System.out.println(liveIndexes);
                    System.out.println("Missing expected headers in live file: " + e.getValue()+ " "+ modelCodeHeader + ", " + e.getKey()+" "+rewardTypeHeader);
                    throw new RuntimeException("Missing expected headers in live file: " + e.getValue() + ", " + e.getKey());
                }

                String modelCode = getCellValueAsString(liveRow.getCell(liveIndexes.get(modelCodeHeader))).trim();
                String rewardType = getCellValueAsString(liveRow.getCell(liveIndexes.get(rewardTypeHeader))).trim();

                if (!modelCode.isEmpty() && !rewardType.isEmpty()) {
                    columnMapping.put(modelCode, rewardType);
                }
            }

            // System.out.println("Column Mapping : "+ columnMapping);

            vlookupMappingArr.add(columnMapping);
        }

        // Processing and writing result rows
        int rowNum = 1;

        for (Map<String, String> masterRow : masterRows) {
            if(vehicleType.length > 0) {

                // Normalizing vehicle type request values
                String vtKey = vehicleType[0].trim().toLowerCase();
                String vtValue = vehicleType[1].trim();

                // Processing the vehicle based solely on the provided vehicle type
                if (!masterRow.containsKey(vtKey)) continue;
                if (!masterRow.get(vtKey).equalsIgnoreCase(vtValue)) continue;

            }

            Row resultRow = resultSheet.createRow(rowNum++);

            // Adding NULL in ID Column
            if (resultIndexes.containsKey("id")) {
                resultRow.createCell(resultIndexes.get("id")).setCellValue("NULL");
            }

            // Copying directly mapped columns from master to result file
            for (Map.Entry<String, String> mapping : normalizedDirectMap.entrySet()) {
                String resultCol = mapping.getKey();
                String masterCol = mapping.getValue();
                int resultColIndex = resultIndexes.getOrDefault(resultCol, -1);
                if (resultColIndex != -1) {
                    String value = masterRow.getOrDefault(masterCol, "");
                    resultRow.createCell(resultColIndex).setCellValue(value);
                    // System.out.println("Master row keys: " + masterRow.keySet());
                }
            }

            // for reward vehicle fuel type column
            if (resultIndexes.containsKey(details[0])) {
                String fuelType = masterRow.getOrDefault(details[1].toLowerCase(), "").trim();
                String fuelMapped = switch (fuelType.toUpperCase()) {
                    case "PETROL", "PETROL+CNG", "PETROL T", "PETROL C" -> "Petrol";
                    case "DIESEL", "DIESEL T", "DIESEL C" -> "Diesel";
                    case "CNG" -> "CNG";
                    case "LPG" -> "LPG";
                    case "ELECTRIC", "BATTERY", "ELECTRICITY", "ELECTRIC T", "ELECTRIC C", "ELECTRIC HYBRID"  -> "Electric";
                    case "HYBRID" -> "Hybrid";
                    default -> "";
                };
                resultRow.createCell(resultIndexes.get(details[0])).setCellValue(fuelMapped);
            }

            // For reward vehicle type column from live file to result file

            int vlookupMappingArridx = 0;

            // System.out.println("Ye hai service wala: "+normalizedVlookupMap);

            for(Map.Entry<String, String> e : normalizedVlookupMap.entrySet()) {

                Map<String, String> currentMapping = vlookupMappingArr.get(vlookupMappingArridx++);

                if (resultIndexes.containsKey(e.getValue()) && resultIndexes.containsKey(e.getKey())) {
                    Cell modelCodeCell = resultRow.getCell(resultIndexes.get(e.getValue()));
                    String modelCode = getCellValueAsString(modelCodeCell).trim();

                    String valueFromMap = currentMapping.getOrDefault(modelCode, "#N/A");

                    // Custom logic ONLY for reward vehicle type
                    String resultColumn = e.getKey();
                    String safeValue;

                    if ("reward_vehicle_type".equalsIgnoreCase(resultColumn)) {
                        // DB Logic only for reward vehicle type
                        StringBuilder sb = new StringBuilder();
                        for (String key : dbParam) {
                            int colIndex = resultIndexes.getOrDefault(key.toLowerCase(), -1);
                            if (colIndex != -1) {
                                Cell cell = resultRow.getCell(colIndex);
                                String value = getCellValueAsString(cell).replaceAll("\\s+", ""); // removes all spaces
                                sb.append(value);
                            }
                        }

                        String concatenatedString = sb.toString();

                        System.out.println("Concatenated string: " + concatenatedString);

                        if(!validRewardVehicleType.contains(valueFromMap)) {
                            if(vehicleMappingDao.containsVehicleModelString(concatenatedString)){
                                valueFromMap = vehicleMappingDao.getRewardVehicleType(concatenatedString);
                            }
                        } else {
                            Mapping newMappedValue = new Mapping(concatenatedString, valueFromMap);
                            if(validRewardVehicleType.contains(valueFromMap)) {
                                String mappingResponse = vehicleMappingDao.saveRewardVehicleType(newMappedValue);
                                System.out.println(mappingResponse);
                            }
                        }

                        safeValue = validRewardVehicleType.contains(valueFromMap) ? valueFromMap : "#N/A";

                    } else {
                        // For all other vlookup columns, just write the mapped value
                        safeValue = valueFromMap;
                    }

                    resultRow.createCell(resultIndexes.get(resultColumn)).setCellValue(safeValue);
                }
            }


            /* For reward vehicle power column (CC calculation - only for Electric vehicles)
                If CC > 1000
                  then reward vehicle power column = CC / 1000
                else
                 reward vehicle power column = CC
            */
            if (resultIndexes.containsKey(details[2])) {
                Cell ccCell = resultRow.getCell(resultIndexes.getOrDefault(details[3], 0));
                Cell fuelTypeCell = resultRow.getCell(resultIndexes.getOrDefault(details[0], 0));
                String ccStr = getCellValueAsString(ccCell).trim();
                String fuelType = getCellValueAsString(fuelTypeCell).trim();

                // System.out.println("Fuel type cell: "+ fuelTypeCell+ "Fuel type: "+ fuelType+ ", CC Value: "+ ccStr);
                // System.out.println("Electric".equalsIgnoreCase(fuelType) && !ccStr.isEmpty());

                if ("Electric".equalsIgnoreCase(fuelType) && !ccStr.isEmpty()) {
                    try {
                        //System.out.println(ccStr);
                        float ccValue = Float.parseFloat(ccStr);
                        float rawCC = ccValue < 1000 ? ccValue : ccValue / 1000f;
                        BigDecimal bd = new BigDecimal(rawCC).setScale(1, RoundingMode.HALF_UP);
                        String finalCC = bd.toPlainString();

                        resultRow.createCell(resultIndexes.get(details[2])).setCellValue(finalCC);
                    } catch (NumberFormatException e) {
                        throw new CubicCapacityException("Cubic capacity must be a Number.");
                    }
                }
            }
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        resultWb.write(out);
        resultWb.close();
        liveWb.close();
        return out;
    }

    // Helper function for accessing and normalizing header row
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

    // Helper function for accessing value to cell
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

    // Helper function for getting specific cell index using cell reference
    private int getColumnIndex(String cellRef) {
        int col = 0;
        for (int i = 0; i < cellRef.length(); i++) {
            if (Character.isDigit(cellRef.charAt(i))) break;
            col = col * 26 + (cellRef.charAt(i) - 'A' + 1);
        }
        return col - 1;
    }
}
