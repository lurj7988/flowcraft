package com.original.flowcraft.shell.utils;

import com.google.common.base.Strings;
import com.original.flowcraft.utils.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class DefaultExcelRowHandler implements IExcelRowHandler {
    private boolean keyLower = false;

    private final Logger logger = LoggerFactory.getLogger(DefaultExcelRowHandler.class);

    public DefaultExcelRowHandler() {

    }

    public DefaultExcelRowHandler(boolean keyLower) {
        this.keyLower = keyLower;
    }

    @Override
    public Map<String, Object> convertRowToData(Row firstRow, Row row) throws Exception {
        Map<String, Object> map = new LinkedHashMap<>();
//        List<Object> columns = new ArrayList<>();
        Map<Integer, Object> columnIndexs = new HashMap<>();
        for (int i = 0; i < firstRow.getLastCellNum(); i++) {
            Object key = convertCellValueToString(row.getRowNum(), firstRow.getCell(i));
            if (StringUtils.isBlank(key)) {
                break;
            }
//            columns.add(Optional.ofNullable(key).orElse(""));
            if (StringUtils.isNotBlank(key) && columnIndexs.containsValue(key)) {
                throw new Exception("表头重复[" + key + "]");
            }
            columnIndexs.put(i, key);
//            if (key != null) {
//
//                String columnName = keyLower ? String.valueOf(key).toLowerCase() : String.valueOf(key);
//                if (map.containsKey(columnName)) {
//                    throw new Exception("表头重复");
//                }
//                map.put(columnName, null);
//            }
        }


        //去头
//        trimStartEmpty(columns);
        //去尾
//        trimEndEmpty(columns);
//        if (columns.contains("")) {
//            throw new Exception("表头有空");
//        }
        boolean isValueNull = true;
//        for (int i = 0; i < row.getLastCellNum(); i++) {
//            Object key = convertCellValueToString(firstRow.getCell(i));
//            //如果读取到表头列为空则抛弃
//            if (key != null) {
//                String columnName = keyLower ? String.valueOf(key).toLowerCase() : String.valueOf(key);
//                Object columnValue = convertCellValueToString(row.getCell(i));
//                //从第一列开始判断如果为空就一直判断，如果有不为空的后面就一直是false
//                if (isValueNull) {
//                    isValueNull = columnValue == null;
//                }
//                map.put(columnName, columnValue);
//            }
//        }
        for (Map.Entry<Integer, Object> columnIndex : columnIndexs.entrySet()) {
            Integer index = columnIndex.getKey();
            Object column = columnIndex.getValue();
            if (StringUtils.isNotBlank(column)) {
                String columnName = keyLower ? String.valueOf(column).toLowerCase() : String.valueOf(column);
                Object columnValue = convertCellValueToString(row.getRowNum(), row.getCell(index));
                //从第一列开始判断如果为空就一直判断，如果有不为空的后面就一直是false
                if (isValueNull) {
                    isValueNull = columnValue == null;
                }
                map.put(columnName, Optional.ofNullable(columnValue).orElse(""));
            }
        }
        if (isValueNull) {
            logger.info(map.toString());
            return null;
        }
        return map;
    }

    private List<Object> trimEndEmpty(List<Object> list) {
        if (list.isEmpty()) {
            return list;
        }
        int index = list.size() - 1;
        if ("".equals(list.get(index))) {
            list.remove(index);
            return trimEndEmpty(list);
        } else {
            return list;
        }
    }

    private List<Object> trimStartEmpty(List<Object> list) {
        if (list.isEmpty()) {
            return list;
        }
        int index = 0;
        if ("".equals(list.get(index))) {
            list.remove(index);
            return trimStartEmpty(list);
        } else {
            return list;
        }
    }

    /*
     * poi特殊日期格式：数字格式化成-yyyy年MM月dd日，格式
     * */
    private static final ArrayList<String> PoiDateList = new ArrayList<String>() {
        {
            add("年");
            add("月");
            add("日");
        }
    };

    /**
     * 将单元格内容转换为字符串
     *
     * @param cell cell
     * @return 内容
     */
    private Object convertCellValueToString(int rowNum, Cell cell) {
        if (cell == null) {
            return null;
        }
        Object returnValue = null;
        switch (cell.getCellType()) {
            case NUMERIC: // 数字
                //Double doubleValue = cell.getNumericCellValue();
                // 格式化科学计数法，取一位整数
                //DecimalFormat df = new DecimalFormat("0");
                //returnValue = df.format(doubleValue);
                //  获取单元格值的格式化信息
                String dataFormat = cell.getCellStyle().getDataFormatString();
                //  判断格式化信息中是否存在：年月日
                AtomicReference<Boolean> isDate = new AtomicReference<>(false);
                if (!Strings.isNullOrEmpty(dataFormat)) {
                    PoiDateList.forEach(x -> isDate.set(isDate.get() || dataFormat.contains(x)));
                }
                if (DateUtil.isCellDateFormatted(cell)) {
                    returnValue = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                            .format(DateUtil.getJavaDate(cell.getNumericCellValue()));
                } else if (DateUtil.isCellInternalDateFormatted(cell)) {
                    returnValue = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                            .format(DateUtil.getJavaDate(cell.getNumericCellValue()));
                }
                //有些情况，时间搓 数字格式化显示为时间,不属于上面两种时间格式
                else if (isDate.get()) {
                    returnValue = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(cell.getDateCellValue());
                }
                //有些情况，时间搓 数字格式化显示为时间,不属于上面两种时间格式
                else if (dataFormat == null) {
                    returnValue = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                            .format(DateUtil.getJavaDate(cell.getNumericCellValue()));
                } else {
                    if (Strings.isNullOrEmpty(dataFormat)) {
                        returnValue = cell.getNumericCellValue();
                    } else {
                        if (cell.getCellStyle().getDataFormatString().contains("$")) {
                            returnValue = "$" + cell.getNumericCellValue();
                        } else if (cell.getCellStyle().getDataFormatString().contains("￥")) {
                            returnValue = "￥" + cell.getNumericCellValue();
                        } else if (cell.getCellStyle().getDataFormatString().contains("¥")) {
                            returnValue = "¥" + cell.getNumericCellValue();
                        } else if (cell.getCellStyle().getDataFormatString().contains("€")) {
                            returnValue = "€" + cell.getNumericCellValue();
                        } else {
                            returnValue = cell.getNumericCellValue();
                        }
                    }
                }
                break;
            case STRING: // 字符串
                returnValue = cell.getStringCellValue();
                break;
            case BOOLEAN: // 布尔
                returnValue = cell.getBooleanCellValue();
                break;
            case BLANK: // 空值
                break;
            case FORMULA: // 公式
                returnValue = new CellFormula(rowNum, cell.getCellFormula());
                break;
            case ERROR: // 故障
                break;
            default:
                break;
        }
        return returnValue;
    }
}
