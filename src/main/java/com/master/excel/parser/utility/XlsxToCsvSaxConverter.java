package com.master.excel.parser.utility;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.xssf.model.SharedStrings;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;

@Component
public class XlsxToCsvSaxConverter {

    public MultipartFile convertToCsv(String originalFilename, InputStream xlsxInput) {
        try (ByteArrayOutputStream csvOut = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(csvOut, StandardCharsets.UTF_8);
             OPCPackage pkg = OPCPackage.open(xlsxInput)) {

            XSSFReader reader = new XSSFReader(pkg);
            StylesTable styles = reader.getStylesTable();
            SharedStrings sst = reader.getSharedStringsTable();
            DataFormatter formatter = new DataFormatter();

            XMLReader parser = XMLReaderFactory.createXMLReader();

            // Our handler writes CSV while respecting missing cells
            XSSFSheetXMLHandler.SheetContentsHandler sheetHandler = new CsvSheetHandler(writer);

            XSSFSheetXMLHandler xmlHandler = new XSSFSheetXMLHandler(
                    styles, /* comments = */ null, sst, sheetHandler, formatter, /* formulasNotResults */ false
            );
            parser.setContentHandler(xmlHandler);

            // Process the first sheet; you can iterate if you want all sheets
            try (InputStream sheet = reader.getSheetsData().next()) {
                parser.parse(new InputSource(sheet));
            }

            writer.flush();

            String csvFilename = (originalFilename != null && originalFilename.endsWith(".xlsx"))
                    ? originalFilename.substring(0, originalFilename.length() - 5) + ".csv"
                    : "converted.csv";

            return new MockMultipartFile("file", csvFilename, "text/csv",
                    new ByteArrayInputStream(csvOut.toByteArray()));

        } catch (Exception e) {
            throw new RuntimeException("Failed to convert XLSX to CSV: " + e.getMessage(), e);
        }
    }

    // Handles sparse cells -> inserts commas for gaps so columns stay aligned
    static class CsvSheetHandler implements XSSFSheetXMLHandler.SheetContentsHandler {
        private final Writer writer;
        private int currentCol;

        CsvSheetHandler(Writer writer) {
            this.writer = writer;
        }

        @Override
        public void startRow(int rowNum) {
            currentCol = 0;
        }

        @Override
        public void endRow(int rowNum) {
            try {
                writer.write("\n");
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public void cell(String cellRef, String formattedValue, XSSFComment comment) {
            int colIndex = cellRefToIdx(cellRef); // e.g., "C5" -> 2
            try {
                // write separators for skipped cells
                while (currentCol < colIndex) {
                    if (currentCol > 0) writer.write(",");
                    currentCol++;
                }
                if (currentCol > 0) writer.write(",");
                writer.write(escapeCsv(formattedValue == null ? "" : formattedValue));
                currentCol++;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public void headerFooter(String text, boolean isHeader, String tagName) {
            // not used
        }

        private static int cellRefToIdx(String cellRef) {
            // Extract column letters (strip row digits)
            int i = 0;
            while (i < cellRef.length() && Character.isLetter(cellRef.charAt(i))) i++;
            String colLetters = cellRef.substring(0, i).toUpperCase();
            int col = 0;
            for (int k = 0; k < colLetters.length(); k++) {
                col = col * 26 + (colLetters.charAt(k) - 'A' + 1);
            }
            return col - 1; // zero-based
        }

        private static String escapeCsv(String v) {
            if (v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
                return "\"" + v.replace("\"", "\"\"") + "\"";
            }
            return v;
        }
    }
}
