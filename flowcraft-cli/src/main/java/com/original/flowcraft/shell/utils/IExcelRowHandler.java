package com.original.flowcraft.shell.utils;

import org.apache.poi.ss.usermodel.Row;

import java.util.Map;

public interface IExcelRowHandler
{
    Map<String, Object> convertRowToData(Row firstRow, Row row) throws Exception;
}
