package com.original.flowcraft.shell.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * excel解析器
 */
@Slf4j
public class ExcelParser {

    private static final String XLS = "xls";
    private static final String XLSX = "xlsx";

    public Map<String, List<Map<String, Object>>> readExcel(String fileName, IExcelRowHandler excelRowHandler) {
        return readExcel(fileName, null, excelRowHandler);
    }

    public Map<String, List<Map<String, Object>>> readExcel(String fileName, Integer firstRowNum, IExcelRowHandler excelRowHandler) {
        Workbook workbook = null;
        FileInputStream inputStream = null;
        try {
            // 获取Excel后缀名
            String fileType = fileName.substring(fileName.lastIndexOf(".") + 1);
            if (!XLS.equalsIgnoreCase(fileType) && !XLSX.equalsIgnoreCase(fileType)) {
                log.warn("解析Excel失败，文件格式不支持！");
                return null;
            }
            // 获取Excel文件
            File excelFile = new File(fileName);
            if (!excelFile.exists()) {
                log.warn("指定的Excel文件不存在！");
                return null;
            }
            // 获取Excel工作簿
            inputStream = new FileInputStream(excelFile);
            workbook = getWorkbook(inputStream, fileType);
            // 读取excel中的数据
            return parseExcel(fileName, workbook, excelRowHandler, firstRowNum);
        } catch (Exception e) {
            log.error("解析Excel失败，文件名：" + fileName, e);
            return null;
        } finally {
            try {
                if (null != workbook) {
                    workbook.close();
                }
                if (null != inputStream) {
                    inputStream.close();
                }
            } catch (Exception e) {
                log.error("关闭数据流出错！错误信息：" + e.getMessage());
            }
        }
    }

    /**
     * 根据文件后缀名类型获取对应的工作簿对象
     *
     * @param inputStream 读取文件的输入流
     * @param fileType    文件后缀名类型（xls或xlsx）
     * @return 包含文件数据的工作簿对象
     * @throws IOException 异常
     */
    public Workbook getWorkbook(InputStream inputStream, String fileType) throws IOException {
        Workbook workbook = null;
        if (fileType.equalsIgnoreCase(XLS)) {
            workbook = new HSSFWorkbook(inputStream);
        } else if (fileType.equalsIgnoreCase(XLSX)) {
            workbook = new XSSFWorkbook(inputStream);
        }
        return workbook;
    }

    private Map<String, List<Map<String, Object>>> parseExcel(String fileName, Workbook workbook, IExcelRowHandler excelRowHandler) {
        return parseExcel(fileName, workbook, excelRowHandler, null);
    }


    /**
     * 解析Excel数据
     *
     * @param workbook Excel工作簿对象
     * @return 解析结果
     */
    private Map<String, List<Map<String, Object>>> parseExcel(String fileName, Workbook workbook, IExcelRowHandler excelRowHandler, Integer firstRowNum) {
        Map<String, List<Map<String, Object>>> map = new LinkedHashMap<>();
        // 解析sheet
        for (int sheetNum = 0; sheetNum < workbook.getNumberOfSheets(); sheetNum++) {
            Sheet sheet = workbook.getSheetAt(sheetNum);
            try {
                // 校验sheet是否合法
                if (sheet == null) {
                    continue;
                }
                // 获取第一行数据
                if (firstRowNum == null) {
                    firstRowNum = sheet.getFirstRowNum();
                }

                Row firstRow = sheet.getRow(firstRowNum);
                if (null == firstRow) {
                    log.warn("解析Excel失败，在第一行没有读取到任何数据！");
                }
                // 解析每一行的数据，构造数据对象
                int rowStart = firstRowNum + 1;
                int rowEnd = sheet.getPhysicalNumberOfRows();
                List<Map<String, Object>> list = new ArrayList<>();
                for (int rowNum = rowStart; rowNum < rowEnd; rowNum++) {
                    Row row = sheet.getRow(rowNum);
                    if (null == row) {
                        continue;
                    }
                    Map<String, Object> resultData = excelRowHandler.convertRowToData(firstRow, row);
                    if (null == resultData) {
                        log.warn("第 " + row.getRowNum() + "行数据不合法，已忽略！");
                        continue;
                    }
                    list.add(resultData);
                }
                if (!list.isEmpty()) {
                    map.put(sheet.getSheetName(), list);
                }
            } catch (Exception e) {
                log.error("解析Excel失败，文件路径：[" + fileName + "]，sheet名称：[" + sheet.getSheetName() + "]" + "，错误信息：" + e.getMessage());
            }
        }
        return map;
    }
}
