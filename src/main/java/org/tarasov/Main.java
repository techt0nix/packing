package org.tarasov;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;

import java.io.*;
import java.util.*;

public class Main {
    public static void createHeaderRow(Row row) {
        row.createCell(0).setCellValue("Model");
        row.createCell(1).setCellValue("Quantity");
        row.createCell(2).setCellValue("Container");
        row.createCell(3).setCellValue("Date");
        row.createCell(4).setCellValue("Serial number");
        row.createCell(5).setCellValue("Bar Code");
        row.createCell(6).setCellValue("Version");
        row.createCell(7).setCellValue("Color");
        row.createCell(8).setCellValue("Order");
    }

    public static void insertValueFromCell(Row doneRow, Row row, int to, int from) {
        if (row.getCell(from) == null) {
            doneRow.createCell(to).setCellType(CellType.STRING);
        } else {
            doneRow.createCell(to).setCellValue(row.getCell(from).getStringCellValue());
        }
    }

    public static void main(String[] args) throws InvalidFormatException, IOException {
        String path;
        Scanner scanner = new Scanner(System.in);
        System.out.println("$/ : Введите путь с файлами для обработки: ");
        long startTime = System.currentTimeMillis();
        path = scanner.nextLine();
        System.out.println("$/ : Обработка...");
        Workbook doneWorkbook;

        File directory = new File(path);

        File[] files = directory.listFiles();
        Map<String, String> notResolvedColors = new HashMap<String, String>();


        for (File file : files) {
            FileInputStream fileInputStream = new FileInputStream(file);
            Workbook workbook = new HSSFWorkbook(fileInputStream);
            Iterator<Row> rows = ExcelReader.getExcelList(workbook);
            doneWorkbook = new HSSFWorkbook();
            Sheet doneSheet = doneWorkbook.createSheet();
            Row headerRow = doneSheet.createRow(0);
            createHeaderRow(headerRow);

            int rowNum = 0;
            int doneRowNum = 0;
            String key, prevKey = null;

            while (rows.hasNext()) {
                Row row = rows.next();
                if (rowNum == 0){
                    rowNum++;
                    doneRowNum++;
                    continue;
                } else if (rowNum == 1) {
                    key = row.getCell(6).getStringCellValue() + " " + row.getCell(4).getStringCellValue();
                    prevKey = key;
                    doneWorkbook.setSheetName(0, key.replaceAll("[^A-Za-zА-Яа-я-^0-9-\\s]", "") + "(" + doneWorkbook.getNumberOfSheets() + ")");
                }

                key = row.getCell(6).getStringCellValue() + " " + row.getCell(4).getStringCellValue();
                if (!key.equals(prevKey)) {
                    doneSheet = doneWorkbook.createSheet(key.replaceAll("[^A-Za-zА-Яа-я-^0-9-\\s-]", "") + "(" + doneWorkbook.getNumberOfSheets() + ")");
                    headerRow = doneSheet.createRow(0);
                    createHeaderRow(headerRow);
                    doneRowNum = 1;
                }

                Iterator<Cell> cellIterator = row.cellIterator();
                Row doneRow = doneSheet.createRow(doneRowNum);

                while (cellIterator.hasNext()) {
                    Cell cell = cellIterator.next();
                    cell.setCellType(CellType.STRING);
                }

                for (int i = 0; i < 9; i++) {
                    doneRow.createCell(i).setCellType(CellType.STRING);
                }

                insertValueFromCell(doneRow, row, 0, 6);
                insertValueFromCell(doneRow, row, 1, 0);
                insertValueFromCell(doneRow, row, 2, 5);
                insertValueFromCell(doneRow, row, 3, 1);
                insertValueFromCell(doneRow, row, 4, 2);
                insertValueFromCell(doneRow, row, 5, 3);
                insertValueFromCell(doneRow, row, 6, 10);

                if (row.getCell(4) != null) {
                    String color;
                    if ((color = ColorResolver.resolveColor(row.getCell(4).getStringCellValue())) == null) {
                        color = row.getCell(4).getStringCellValue();
                        doneRow.getCell(7).setCellValue(color);

                        notResolvedColors.put(row.getCell(4).getStringCellValue() + " " +
                                row.getCell(6).getStringCellValue() +
                                " " +
                                file.getName(), row.getCell(4).getStringCellValue());
                    } else {
                        doneRow.getCell(7).setCellValue(color);
                    }
                }


                insertValueFromCell(doneRow, row, 8, 7);
                prevKey = key;
                rowNum++;
                doneRowNum++;
            }

            File outputFile = new File("C:\\Users\\tarasov.a\\Desktop\\files\\" + file.getName().substring(0, file.getName().indexOf(".")) + ".xls");
            FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
            doneWorkbook.write(fileOutputStream);
            fileOutputStream.close();
            fileInputStream.close();
        }

        if (!notResolvedColors.isEmpty()) {
            System.out.println("$/ : Цвета неопределены:");
            for (String key : notResolvedColors.keySet()) {
                System.out.println(key);
            }
            addNotResolvedColors(notResolvedColors);

        } else {
            System.out.println("$/ : Все цвета определены, процесс успешно завершен");
        }
        System.out.println("$/ : Процесс занял " + (System.currentTimeMillis() - startTime) + " мс");
    }

    public static void addNotResolvedColors(Map<String, String> notResolvedColors) throws IOException{
        Map<String, Integer> uniqueColors = new HashMap<String, Integer>();

        int count = 0;
        for (String color : notResolvedColors.values()) {
            uniqueColors.put(color, count++);
        }

        File patterns = new File("C:\\Users\\tarasov.a\\Desktop\\parser\\patterns.xls");
        FileInputStream fio = new FileInputStream(patterns);
        Workbook patternWorkbook = new HSSFWorkbook(fio);
        Sheet sheet = patternWorkbook.getSheetAt(0);
        int lastRowNum = sheet.getLastRowNum();

        for (String color : uniqueColors.keySet()) {
            Row row = sheet.createRow(lastRowNum + 1);
            Cell cell = row.createCell(0);
            cell.setCellType(CellType.STRING);
            cell.setCellValue(color);
            lastRowNum++;
        }

        fio.close();
        FileOutputStream fileOutputStream = new FileOutputStream(patterns);
        ((HSSFWorkbook) patternWorkbook).write(fileOutputStream);
        patternWorkbook.close();
        fileOutputStream.close();
        System.out.println("$/ : Недостающие цвета добавлены в patterns.xls, переведите их, прежде чем снова запускать приложение");
    }
}