package com.original.flowcraft.shell.utils;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

public class ExcelExporter {

    private static final Logger logger = LoggerFactory.getLogger(ExcelExporter.class);

    public static void convertList2Excel(Map<String, List<Map<String, Object>>> map, String filepath, String filename) {
        convertList2Excel(map, filepath, filename, HorizontalAlignment.LEFT);
    }

    public static void convertList2Excel(Map<String, List<Map<String, Object>>> map, String filepath, String filename, HorizontalAlignment align) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            for (Entry<String, List<Map<String, Object>>> excel : map.entrySet()) {
                createSheet(wb, excel.getKey(), excel.getValue(), align);
            }
            createExcel(wb, filepath, filename);
        } catch (Exception e) {
            logger.error(filename + "创建异常:{}", e.getMessage());
        }
    }

    public static void convertList2Excel(List<Map<String, Object>> list, String filepath, String filename) {
        convertList2Excel(list, filepath, filename, HorizontalAlignment.CENTER);
    }

    public static void convertList2Excel(List<Map<String, Object>> list, String filepath, String filename, HorizontalAlignment align) {
        convertList2Excel(list, "Sheet1", filepath, filename, align);
    }

    public static void convertList2Excel(List<Map<String, Object>> list, String sheetname, String filepath, String filename, HorizontalAlignment align) {
        try {
            XSSFWorkbook wb = new XSSFWorkbook(); // 创建一个Excel文件
            createSheet(wb, sheetname, list, align);
            createExcel(wb, filepath, filename);
        } catch (Exception e) {
            logger.error("导出excel异常", e);
        }
    }

    public static void createExcel(XSSFWorkbook wb, String filepath, String filename) {
        // 判断是否存在目录. 不存在则创建
        isChartPathExist(filepath);
        try (FileOutputStream output = new FileOutputStream(filepath + filename)) {
            wb.write(output);// 写入磁盘
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    private static void createSheet(XSSFWorkbook wb, String sheetname, List<Map<String, Object>> list) {
        createSheet(wb, sheetname, list, HorizontalAlignment.CENTER);
    }

    public static void createSheet(XSSFWorkbook wb, String sheetname, List<Map<String, Object>> list, HorizontalAlignment align) {
        XSSFSheet sheet = wb.createSheet(sheetname);// 创建一个工作簿
        XSSFRow catalog = sheet.createRow(0);// 行数设置
        List<String> columns = new ArrayList<>();
        XSSFCellStyle cellStyle = getXSSFCellStyle(wb, align);
        if (!list.isEmpty()) {
            Map<String, Object> record = list.get(0);
            int i = 0;
            for (Entry<String, Object> entry : record.entrySet()) {
                XSSFCell cell = catalog.createCell(i);
                cell.setCellValue(entry.getKey());
                cell.setCellStyle(cellStyle);
                columns.add(entry.getKey());
                //宽度自适应
                sheet.autoSizeColumn(i);
                i++;
            }
        }
        int totalRows = 1; // 行数控制
        XSSFCell cell;
        for (Map<String, Object> record : list) {
            XSSFRow row = sheet.createRow(totalRows);// 第三行放值
            row.setHeight((short) -1);
            for (int i = 0; i < columns.size(); i++) {
                cell = row.createCell(i);
                Object value = record.get(columns.get(i));
                if (value instanceof String) {
                    cell.setCellValue(String.valueOf(value));
                } else if (value instanceof Double) {
                    cell.setCellValue((double) value);
                } else if (value instanceof Date) {
                    cell.setCellValue((Date) value);
                } else if (value instanceof CellFormula cellFormula) {
                    cell.setCellFormula(cellFormula.formula());
                } else {
                    cell.setCellValue(Optional.ofNullable(record.get(columns.get(i))).orElse("") + "");
                }
                cell.setCellStyle(cellStyle);
            }
            totalRows++;
        }
    }

    public static XSSFCellStyle getXSSFCellStyle(XSSFWorkbook wb, HorizontalAlignment align) {
        XSSFCellStyle style = getBaseStyle(wb);
        style.setAlignment(align);
        style.setVerticalAlignment(VerticalAlignment.CENTER);// 垂直居中
        style.setWrapText(true);// 开启自动换行需要
        return style;
    }

    public static XSSFCellStyle getCenterStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = getXSSFCellStyle(wb, HorizontalAlignment.CENTER);
        //style.setAlignment(HorizontalAlignment.CENTER);// 水平居中
        style.setVerticalAlignment(VerticalAlignment.CENTER);// 垂直居中
        // style.setWrapText(true);// 开启自动换行需要
        return style;
    }

    public static XSSFCellStyle getLeftStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = getXSSFCellStyle(wb, HorizontalAlignment.LEFT);
        //style.setAlignment(HorizontalAlignment.LEFT);// 水平居左
        style.setVerticalAlignment(VerticalAlignment.CENTER);// 垂直居中
        style.setWrapText(true);// 开启自动换行需要
        return style;
    }

    public static XSSFCellStyle getBaseStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setFontHeightInPoints((short) 10);
        font.setFontName("宋体");
        style.setFont(font);
        // 加边框
        style.setBorderBottom(BorderStyle.THIN);// 下边框
        style.setBorderLeft(BorderStyle.THIN);// 左边框
        style.setBorderRight(BorderStyle.THIN);// 右边框
        style.setBorderTop(BorderStyle.THIN); // 上边框
        return style;
    }

    public static void isChartPathExist(String path) {
        File parentFile = new File(path);
        if (!parentFile.exists()) {
            logger.info(String.valueOf(parentFile.mkdirs()));
        }
    }
}
