package com.master.excel.parser.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.master.excel.parser.dao.VehicleMapping2WDao;
import com.master.excel.parser.dao.VehicleMapping4WDao;
import com.master.excel.parser.dao.VehicleMappingCVDao;
import com.master.excel.parser.dto.MakeModelCode;
import com.master.excel.parser.dto.VehicleMapping2W;
import com.master.excel.parser.dto.VehicleMapping4W;
import com.master.excel.parser.dto.VehicleMappingCV;
import com.master.excel.parser.exception.VehicleTypeUndefined;
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
import java.util.stream.Collectors;

@Service
@Transactional
public class ExcelService {

    @Autowired
    VehicleMappingCVDao vehicleMappingCVDao;

    @Autowired
    VehicleMapping2WDao vehicleMapping2WDao;

    @Autowired
    VehicleMapping4WDao vehicleMapping4WDao;

    @Autowired
    GeminiService geminiService;

    private final ObjectMapper mapper = new ObjectMapper();

    public ByteArrayOutputStream automateExcelPopulation(
            MultipartFile masterFile,
            MultipartFile liveFile,
            MultipartFile resultTemplateFile,
            Map<String, String> directMap,
            Map<String, String> vlookupMap,
            String[] details,
            String[] vehicleType,
            String[] dbParam,
            String[] fixedValues
            ) throws Exception {

        // Load Make Model Code for 4W
        List<MakeModelCode> list = new ArrayList<>();
        if(details[4].equals("4W"))
             list = vehicleMapping4WDao.getMakeModelCode();
        else if (details[4].equals("2W")) {
            list = vehicleMapping2WDao.getMakeModelCode();
        } else {
            list = vehicleMappingCVDao.getMakeModelCode();
        }

        Map<String, MakeModelCode> makeCodeMap = new HashMap<>();
        Map<String, MakeModelCode> modelCodeMap = new HashMap<>();
        makeCodeMap =
                list.stream().collect(Collectors.toMap(
                        dto -> dto.getMakeName().toLowerCase().trim(),
                        dto -> dto,
                        (oldVal, newVal) -> {
                            if (isMakeCodeBlank(oldVal)) {
                                return newVal;
                            }
                            return oldVal;
                        }
                        ));

        modelCodeMap = list.stream()
                .collect(Collectors.toMap(
                        dto -> dto.getMakeName().toLowerCase().trim() + " | " + dto.getModelName().toLowerCase().trim(),
                        dto -> dto,
                        (oldVal, newVal) -> {
                            if(isModelCodeBlank(oldVal)) {
                                return newVal;
                            }
                            return oldVal;
                        }
                        ));


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
//        ArrayList<Map<String, String>> bundleForAI = new ArrayList<>();
        for (Map<String, String> masterRow : masterRows) {

            if(vehicleType.length > 0) {
                String vehicleTypeKey = vehicleType[0].trim().toLowerCase();

                // Check for blank value in column.
                if(masterRow.get(vehicleTypeKey) == null
                        || masterRow.get(vehicleTypeKey).isEmpty()
                        || masterRow.get(vehicleTypeKey).isBlank()
                        || masterRow.get(vehicleTypeKey).equalsIgnoreCase("")) {
                    throw new VehicleTypeUndefined("Vehicle type columns are blank...");
                }

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
            // Yaha bhi development chal raha
            // Map<String, String> rowVlookupValues = new HashMap<>();
            // yaha tak

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

//                   if("NULL".equalsIgnoreCase(valueFromMap) || "#N/A".equalsIgnoreCase(valueFromMap)){

//                        if("ft_make_code".equalsIgnoreCase(rewardHeader)) {
//                            Cell makeCell = resultRow.getCell(resultIndexes.get(dbParam[0].toLowerCase()));
//
//                            String makeName = getCellValueAsString(makeCell).toLowerCase().trim();
//
//                            MakeModelCode dto = makeCodeMap.get(makeName);
//                            if (dto != null)
//                                System.out.println(makeName + " -> " + dto.getMakeName());
//
//                            safeValue = (dto != null && dto.getMakeCode() != null)
//                                    ? dto.getMakeCode().toString()
//                                    : "NULL";
//                        } else if ("ft_model_code".equalsIgnoreCase(rewardHeader)) {
//                            Cell makeCell = resultRow.getCell(resultIndexes.get(dbParam[0].toLowerCase()));
//                            Cell modelCell = resultRow.getCell(resultIndexes.get(dbParam[1].toLowerCase()));
//
//                            String makeName = getCellValueAsString(makeCell).toLowerCase().trim();
//                            String modelName = getCellValueAsString(modelCell).toLowerCase().trim();
//
//                            System.out.println(makeName + " : " + modelName);
//
//                            MakeModelCode dto = modelCodeMap.get(makeName + " | " + modelName);
//
//                            safeValue = (dto != null && dto.getModelCode() != null)
//                                    ? dto.getModelCode().toString()
//                                    : "NULL";
//                        }
//                    }

                    // Yaha chal raha development abhi
/*
                    String safeValue = valueFromMap;

                    rowVlookupValues.put(rewardHeader, valueFromMap);

                    if(("reward_vehicle_type".equalsIgnoreCase(rewardHeader))) {
                        safeValue = handleRewardVehicleType(resultRow, resultIndexes, dbParam, valueFromMap, details);
                    }
                    if(("vehicle_power_bi".equalsIgnoreCase(rewardHeader))) {
                        safeValue = handleVehiclePowerBi(resultRow, resultIndexes, dbParam, valueFromMap, details);
                    }
                    if(("reward_model".equalsIgnoreCase(rewardHeader))) {
                        safeValue = handleRewardModel(resultRow, resultIndexes, dbParam, valueFromMap, details);
                    }
                    if(("reward_vehicle_fuel_type".equalsIgnoreCase(rewardHeader))) {
                        safeValue = handleRewardVehicleFuelType(resultRow, resultIndexes, dbParam, valueFromMap, details);
                    }

                    // Yaha tak

 */
                    // Call Gemini if Reward_vehicle_type is not found in previous_live_master and current master

//                    if (safeValue.equals("#N/A") && (details[4].equals("4W") || details[4].equals("2W"))) {
//                        String prompt = getPrompt(details[4], masterRow);
//                        System.out.println("Ye value fetch ki hai AI se...");
//                        bundleForAI.add(masterRow);
//                        try{
//                            safeValue = geminiService.askGemini(prompt, "Single");
//                            handleRewardVehicleType(resultRow, resultIndexes, dbParam, modelCode, safeValue, details);
//                        } catch (Exception ex) {
//                            System.out.println("Error from Gemini API: " + ex.getMessage());
//                        }
//                    }

                    resultRow.createCell(resultIndexes.get(rewardHeader)).setCellValue(safeValue);
                }
            }

            // yaha bhi development chal raha
/*
            saveRowInDb(resultRow, resultIndexes, dbParam, rowVlookupValues, details);
*/
            // Yaha tak

            // Electric CC → Power
            if (resultIndexes.containsKey(details[2])) {
                Cell ccCell = resultRow.getCell(resultIndexes.getOrDefault(details[3].toLowerCase(), 0));
                Cell fuelTypeCell = resultRow.getCell(resultIndexes.getOrDefault(details[0], 0));
                String ccStr = getCellValueAsString(ccCell).trim().replace(",", "");
                String fuelType = getCellValueAsString(fuelTypeCell).trim();

                if ("Electric".equalsIgnoreCase(fuelType)) {
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
            // Filling hardcore values
            for(int i = 0; (i + 1) < fixedValues.length; i += 2) {
                resultRow.createCell(resultIndexes.get(fixedValues[i])).setCellValue(fixedValues[i+1]);
            }

            // Filling ft_make and ft_model Code
            if(resultIndexes.containsKey("ft_make_code")) {

                System.out.println("dbParam[0] = " + dbParam[0]);
                System.out.println("Looking for column = " + dbParam[0].toLowerCase());
                System.out.println("Available resultIndexes = " + resultIndexes);


                Cell makeCell = resultRow.getCell(resultIndexes.get(dbParam[0].toLowerCase()));

                String makeName = getCellValueAsString(makeCell).toLowerCase().trim();

                MakeModelCode dto = makeCodeMap.get(makeName);
                if (dto != null)
                    System.out.println(makeName + " -> " + dto.getMakeName());

                String makeCode = (dto != null && dto.getMakeCode() != null)
                        ? dto.getMakeCode().toString()
                        : "NULL";

                resultRow.createCell(resultIndexes.get("ft_make_code")).setCellValue(makeCode);
            }

            if(resultIndexes.containsKey("ft_model_code")) {
                Cell makeCell = resultRow.getCell(resultIndexes.get(dbParam[0].toLowerCase()));
                Cell modelCell = resultRow.getCell(resultIndexes.get(dbParam[1].toLowerCase()));

                String makeName = getCellValueAsString(makeCell).toLowerCase().trim();
                String modelName = getCellValueAsString(modelCell).toLowerCase().trim();

                System.out.println(makeName + " : " + modelName);

                MakeModelCode dto = modelCodeMap.get(makeName + " | " + modelName);

                String modelCode = (dto != null && dto.getModelCode() != null)
                        ? dto.getModelCode().toString()
                        : "NULL";

                resultRow.createCell(resultIndexes.get("ft_model_code")).setCellValue(modelCode);
            }
        }


        // Parsing Gemini response into Row Mapping
//        String batchPrompt = getBatchPrompt(details[4], bundleForAI);
//        String resultJson = geminiService.askGemini(batchPrompt, "Bundle");
//        resultJson = sanitizeGeminiJson(resultJson);
//        List<Map<String,Object>> outRows = mapper.readValue(
//                resultJson,
//                new TypeReference<List<Map<String,Object>>>(){}
//        );

        // Creating sheet for AI fetched data
//        writeAISheet(resultWb, outRows);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        resultWb.write(out);
        resultWb.close();
        return out;
    }

    // Yaha bhi chal raha development
/*
    private String handleRewardVehicleType(Row resultRow, Map<String, Integer> resultIndexes, String[] dbParam,
                                           String valueFromMap, String[] details) {
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
            case "2W" -> valueFromMap = vehicleMapping2WDao.getRewardVehicleType(concatenatedString);
            case "4W" -> valueFromMap = vehicleMapping4WDao.getRewardVehicleType(concatenatedString);
            case "CV" -> valueFromMap = vehicleMappingCVDao.getRewardVehicleType(concatenatedString, details[5].trim());
        }
        return ("NULL".equals(valueFromMap) || "#N/A".equals(valueFromMap)) ? "#N/A" : valueFromMap;

    }

    private String handleRewardVehicleFuelType(Row resultRow, Map<String, Integer> resultIndexes, String[] dbParam,
                                               String valueFromMap, String[] details) {
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
            case "2W" -> valueFromMap = vehicleMapping2WDao.getRewardVehicleFuelType(concatenatedString);
            case "4W" -> valueFromMap = vehicleMapping4WDao.getRewardVehicleFuelType(concatenatedString);
            case "CV" -> valueFromMap = vehicleMappingCVDao.getRewardVehicleFuelType(concatenatedString, details[5].trim());
        }
        return ("NULL".equals(valueFromMap) || "#N/A".equals(valueFromMap)) ? "#N/A" : valueFromMap;

    }

    private String handleRewardModel(Row resultRow, Map<String, Integer> resultIndexes, String[] dbParam,
                                     String valueFromMap, String[] details) {
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
            case "2W" -> valueFromMap = vehicleMapping2WDao.getRewardModel(concatenatedString);
            case "4W" -> valueFromMap = vehicleMapping4WDao.getRewardModel(concatenatedString);
            case "CV" -> valueFromMap = vehicleMappingCVDao.getRewardModel(concatenatedString, details[5].trim());
        }
        return ("NULL".equals(valueFromMap) || "#N/A".equals(valueFromMap)) ? "#N/A" : valueFromMap;

    }

    private String handleVehiclePowerBi(Row resultRow, Map<String, Integer> resultIndexes, String[] dbParam,
                                        String valueFromMap, String[] details) {
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
            case "2W" -> valueFromMap = vehicleMapping2WDao.getVehiclePowerBi(concatenatedString);
            case "4W" -> valueFromMap = vehicleMapping4WDao.getVehiclePowerBi(concatenatedString);
            case "CV" -> valueFromMap = vehicleMappingCVDao.getVehiclePowerBi(concatenatedString, details[5].trim());
        }
        return ("NULL".equals(valueFromMap) || "#N/A".equals(valueFromMap)) ? "#N/A" : valueFromMap;
    }

    private void saveRowInDb(Row resultRow, Map<String, Integer> resultIndexes, String[] dbParam, Map<String, String> rowVlookupValues, String[] details) {

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
            case "2W" -> save2wData(concatenatedString, rowVlookupValues, details[4].trim(), details[5].trim());
            case "4W" -> save4wData(concatenatedString, rowVlookupValues, details[4].trim(), details[5].trim());
            case "CV", "PCV", "GCV", "MISD" -> saveCvData(concatenatedString, rowVlookupValues, details[4].trim(), details[5].trim());
        }
    }

    private void saveCvData(String concatenatedString, Map<String, String> rowVlookupValues, String product, String ic) {
        Boolean contains = vehicleMappingCVDao.containsVehicleModelString(concatenatedString, ic);

        //System.out.println("Concat : " + concatenatedString + ", valueFromMap : " + valueFromMap + ", product : " + product + ", IC : " + ic);

        if (!"NULL".equals(rowVlookupValues.get("reward_vehicle_type")) && !"#N/A".equals(rowVlookupValues.get("reward_vehicle_type")) && !contains) {
            VehicleMappingCV data = new VehicleMappingCV(
                    concatenatedString,
                    rowVlookupValues.get("reward_vehicle_type"),
                    product,
                    rowVlookupValues.getOrDefault("vehicle_power_bi", null),
                    rowVlookupValues.getOrDefault("reward_vehicle_fuel_type", null),
                    rowVlookupValues.getOrDefault("reward_model", null),
                    ic
            );
            vehicleMappingCVDao.save(data, ic);
        }
    }

    private void save4wData(String concatenatedString, Map<String, String> rowVlookupValues, String product, String ic) {
        Boolean contains = vehicleMapping4WDao.containsVehicleModelString(concatenatedString);

        System.out.println("Naya waala");
        System.out.println(
                "Concat : " + concatenatedString
                        + ", product : " + product
                        + ", IC : " + ic
                        + ", Reward Vehicle Type: " + rowVlookupValues.get("reward_vehicle_type")
                        + ", Vehicle Power BI ; " + rowVlookupValues.get("vehicle_power_bi")
                        + ", Reward vehicle fuel type : " + rowVlookupValues.get("reward_vehicle_fuel_type")
                        + ", Reward model : " + rowVlookupValues.get("reward_model")
        );

        if (!"NULL".equals(rowVlookupValues.get("reward_vehicle_type")) && !"#N/A".equals(rowVlookupValues.get("reward_vehicle_type")) && !contains) {
            VehicleMapping4W data = new VehicleMapping4W(
                    concatenatedString,
                    rowVlookupValues.get("reward_vehicle_type"),
                    product,
                    ic,
                    rowVlookupValues.getOrDefault("vehicle_power_bi", null),
                    rowVlookupValues.getOrDefault("reward_vehicle_fuel_type", null),
                    rowVlookupValues.getOrDefault("reward_model", null)
            );
            vehicleMapping4WDao.save(data);
        }
    }

    private void save2wData(String concatenatedString, Map<String, String> rowVlookupValues,String product, String ic) {
        Boolean contains = vehicleMapping2WDao.containsVehicleModelString(concatenatedString);

        //System.out.println("Concat : " + concatenatedString + ", valueFromMap : " + valueFromMap + ", product : " + product + ", IC : " + ic);

        if (!"NULL".equals(rowVlookupValues.get("reward_vehicle_type")) && !"#N/A".equals(rowVlookupValues.get("reward_vehicle_type")) && !contains) {
            VehicleMapping2W data = new VehicleMapping2W(
                    concatenatedString,
                    rowVlookupValues.get("reward_vehicle_type"),
                    product,
                    rowVlookupValues.getOrDefault("vehicle_power_bi", null),
                    rowVlookupValues.getOrDefault("reward_vehicle_fuel_type", null),
                    rowVlookupValues.getOrDefault("reward_model", null),
                    ic
            );
            vehicleMapping2WDao.save(data);
        }
    }
*/

    // Yahe tak hai bhai

    // --- Helpers ---

    private Sheet getOrCreateSheet(Workbook wb) {
        Sheet sheet = wb.getSheet("AI_Classifications");
        if (sheet != null) {
            return sheet; // already exists → use it
        }
        return wb.createSheet("AI_Classifications");
    }


    private void writeAISheet(Workbook resultWb, List<Map<String,Object>> outRows) {

        Sheet aiSheet = getOrCreateSheet(resultWb);

        System.out.println("AI Sheet ban rahi hai....");

        if (outRows == null || outRows.isEmpty()) {
            // create only header with basic columns
            Row header = aiSheet.createRow(0);
            header.createCell(0).setCellValue("No AI Rows Found");
            return;
        }

        // Collect all keys from "input"
        Set<String> headerSet = new LinkedHashSet<>();
        for (Map<String,Object> row : outRows) {
            Map<String,Object> inputMap = (Map<String,Object>) row.get("input");
            if (inputMap != null) headerSet.addAll(inputMap.keySet());
        }
        headerSet.add("reward_vehicle_type"); // Last column

        // Convert to list
        List<String> headers = new ArrayList<>(headerSet);

        // --- Write Header Row ---
        Row headerRow = aiSheet.createRow(0);
        for (int i = 0; i < headers.size(); i++) {
            headerRow.createCell(i).setCellValue(headers.get(i));
        }

        // --- Write Data Rows ---
        int r = 1;
        for (Map<String,Object> rowData : outRows) {
            Row excelRow = aiSheet.createRow(r++);

            Map<String,Object> inputMap = (Map<String,Object>) rowData.get("input");

            for (int c = 0; c < headers.size(); c++) {
                String h = headers.get(c);
                String value = "";

                if ("reward_vehicle_type".equals(h)) {
                    value = String.valueOf(rowData.get("reward_vehicle_type"));
                } else if (inputMap != null && inputMap.containsKey(h)) {
                    value = String.valueOf(inputMap.get(h));
                }

                excelRow.createCell(c).setCellValue(value);
            }
        }

        // Auto-size
        for (int i = 0; i < headers.size(); i++) {
            aiSheet.autoSizeColumn(i);
        }
    }


    private String getPrompt(String type, Map<String, String> row) {
        String mmv = buildStructuredRow(row);
        String carPrompt = "You will be given four inputs: make , model , variant , fuel type , and engine CC of a vehicle. \n" +
                "Your job is to determine the approximate on-road cost of the vehicle in India using general automotive knowledge. \n" +
                "After estimating the cost, respond with ONLY ONE OF THE FOLLOWING TEXT VALUES:\n" +
                "\n" +
                "\"HEV\"  → if the estimated on-road price is MORE than 50 lakh INR  \n" +
                "\"NONHEV\" → if the estimated on-road price is LESS than or equal to 50 lakh INR\n" +
                "\n" + "Find Vehicle details = " + mmv + " \n" +
                "Return strictly and only one of the two texts with no explanations, no extra words, and no formatting.\n";
        String bikePrompt = "You will be given three inputs: make, model, and variant of a two-wheeler sold in India.\n" +
                "Determine whether the vehicle is a \"Bike\" or a \"Scooter\" using general automotive knowledge.\n" +
                "\n" +
                "Return ONLY ONE of the following two text values:\n" +
                "\n" +
                "\"Bike\"\n" +
                "\"Scooter\"\n" +
                "\n" + "Find Vehicle details = " + mmv + " \n" +
                "Do not add explanations, extra words, punctuation, or formatting. Respond with exactly one word only.\n";

        if ("2W".equals(type)) return bikePrompt;
        else if ("4W".equals(type)) return carPrompt;
        else return "";

    }

    private String buildStructuredRowList(List<Map<String, String>> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("Vehicle Data List:\n");

        int index = 1;
        for (Map<String, String> row : rows) {
            sb.append(index++).append(". {\n");
            for (Map.Entry<String, String> e : row.entrySet()) {
                sb.append("  ").append(e.getKey()).append(": ")
                        .append(e.getValue() == null ? "" : e.getValue().trim())
                        .append("\n");
            }
            sb.append("}\n\n");
        }
        return sb.toString();
    }

    private String sanitizeGeminiJson(String raw) {

        System.out.println("Gemini Raw resposne for bundle  : " + raw);

        if (raw == null) return "";

        // Remove markdown backticks
        raw = raw.replace("```json", "")
                .replace("```", "");

        // Remove weird control characters
        raw = raw.replaceAll("[\\x00-\\x1F&&[^\\n\\t]]", "");

        // Trim spaces
        raw = raw.trim();

        // Extract the JSON array if model wrapped it in text
        int start = raw.indexOf('[');
        int end = raw.lastIndexOf(']');
        if (start >= 0 && end >= 0 && end > start) {
            raw = raw.substring(start, end + 1);
        }

        return raw;
    }

    private boolean isModelCodeBlank(MakeModelCode dto) {
        return dto.getModelCode() == null || dto.getModelCode().toString().trim().isEmpty();
    }

    private boolean isMakeCodeBlank(MakeModelCode dto) {
        return dto.getMakeCode() == null || dto.getMakeCode().toString().trim().isEmpty();
    }

    private String getBatchPrompt(String type, List<Map<String, String>> rows) {

        String structuredRows = buildStructuredRowList(rows);

        String rules =
                """
                OUTPUT RULES (IMPORTANT):
                - Output MUST be ONLY a valid JSON array.
                - Do NOT include any explanation, text, comments, backticks, markdown, or notes.
                - Do NOT include trailing commas.
                - Do NOT include extra line breaks in keys or values.
                - All keys MUST be enclosed in double quotes.
                - All string values MUST be in double quotes.
                - JSON must be fully valid and parsable by Jackson.
    
                JSON FORMAT:
                [
                  {
                    "input": { ...original row... },
                    "reward_vehicle_type": "HEV" | "NONHEV" | "Bike" | "Scooter"
                  }
                ]
                """;

        String carRule =
                """
                Classification rule for 4W:
                - If estimated on-road price in India > 50 lakh INR → "HEV"
                - Else → "NONHEV"
                """;

        String bikeRule =
                """
                Classification rule for 2W:
                - If it is a motorcycle → "Bike"
                - If it is a scooter → "Scooter"
                """;

        return structuredRows + "\n" + rules + "\n" + (type.equals("4W") ? carRule : bikeRule);
    }



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

    private String buildStructuredRow(Map<String, String> row) {
        StringBuilder sb = new StringBuilder();
//        System.out.println(row);
        sb.append("Vehicle Data:\n");

        for (Map.Entry<String, String> entry : row.entrySet()) {
            sb.append(entry.getKey())
                    .append(": ")
                    .append(entry.getValue() == null ? "" : entry.getValue().trim())
                    .append("\n");
        }

        return sb.toString();
    }


    private Integer findHeaderIndex(Map<Integer, String> headers, String name) {
        return headers.entrySet().stream()
                .filter(e -> e.getValue().equalsIgnoreCase(name))
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);
    }

    private String mapFuelType(String fuelType) {
        return switch (fuelType.toUpperCase()) {
            case "1", "PETROL", "PETROL WITH LPG", "PETROL WITH CNG", "PETROL+CNG", "PETROL T", "PETROL C", "PH", "PETROL(P)", "PETROL HYBRID(PH)", "PETROL P", "PETROL G", "PETROL+LPG", "P" -> "Petrol";
            case "2", "DIESEL", "DIESEL T", "DIESEL C", "DH", "DIESEL HYBRID(DH)", "DIESEL(D)", "DIESEL P", "DIESEL G", "D" -> "Diesel";
            case "8", "3", "CNG", "CH", "CNG(C)", "C", "LPG/CNG" -> "CNG";
            case "9", "4", "LPG", "LPG(L)" -> "LPG";
            case "LNG" -> "LNG";
            case "7", "ELECTRIC", "ELECTRICAL", "BATTERY", "ELECTRICITY", "ELECTRIC T", "ELECTRIC C", "ELECTRIC HYBRID", "BATTERY(B)", "BATTERY OPERATED", "ELECTRIC P", "B" -> "Electric";
            case "5", "HYBRID", "HYBRID(H)", "MILD HYBRID", "PLUG IN HYBRID", "HYBRID ELECTRIC VEHICLE" -> "Hybrid";
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
            case "2W" -> valueFromMap = handle2wData(concatenatedString, valueFromMap, details[4].trim(), details[5].trim());
            case "4W" -> valueFromMap = handle4wData(concatenatedString, valueFromMap, details[4].trim(), details[5].trim());
            case "CV", "PCV", "GCV", "MISD", "TRAILER" -> valueFromMap = handleCvData(concatenatedString, valueFromMap, details[4], details[5]);
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
