// 🟡 ExcelParser.java
package com.rentalapp.utils;

import android.util.Log;
import com.rentalapp.bean.House;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ExcelParser {

    private static final String TAG = "ExcelParser";

    /**
     * Parse Excel file into a list of House objects.
     * No geocoding or database insertion happens here.
     */
    // ✅Changed: 重写 parseExcel 方法，允许任意列顺序
    public static List<House> parseExcel(InputStream inputStream) {
        List<House> houseList = new ArrayList<>();
        try {
            Workbook workbook = new XSSFWorkbook(inputStream);
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            // ✅ 1. 获取表头映射
            if (!rows.hasNext()) {
                Log.e(TAG, "❌ Excel has no rows");
                return houseList;
            }

            Row headerRow = rows.next();
            int colCount = headerRow.getLastCellNum();
            java.util.Map<String, Integer> colMap = new java.util.HashMap<>();

            for (int i = 0; i < colCount; i++) {
                Cell cell = headerRow.getCell(i);
                if (cell != null) {
                    String name = cell.getStringCellValue().trim().toLowerCase();
                    colMap.put(name, i);
                }
            }

            int rowIndex = 1;
            while (rows.hasNext()) {
                Row row = rows.next();
                rowIndex++;

                try {
                    House house = new House();

                    house.setTitle(getCellString(row, colMap.get("title")));
                    house.setAddress(getCellString(row, colMap.get("address")));
                    house.setPrice((int) getCellNumeric(row, colMap.get("price")));
                    house.setArea((int) getCellNumeric(row, colMap.get("area")));
                    house.setHousetype(getCellString(row, colMap.get("housetype")));
                    house.setPowerrate(getCellString(row, colMap.get("powerrate")));
                    house.setRemark(getCellString(row, colMap.get("remark")));

                    // ✅ 检查必填字段
                    if (house.getTitle().isEmpty() || house.getAddress().isEmpty() || house.getPrice() <= 0) {
                        Log.w(TAG, "⚠️ Skipped row " + rowIndex + ": missing required fields");
                        continue;
                    }

                    house.setStatus("nocheck");
                    house.setImgpath("");
                    house.setPdfpath("");
                    house.setUname("landlord");
                    house.setUphone("");
                    house.setUmail("");
                    house.setLng("0");
                    house.setLat("0");

                    houseList.add(house);
                    Log.i(TAG, "✅ Parsed house from row " + rowIndex + ": " + house.getTitle());

                } catch (Exception e) {
                    Log.e(TAG, "❌ Error parsing row " + rowIndex, e);
                }
            }

            workbook.close();
        } catch (Exception e) {
            Log.e(TAG, "❌ Excel parse error", e);
        }

        return houseList;
    }


    private static String getCellString(Row row, int index) {
        Cell cell = row.getCell(index);
        return cell == null ? "" : cell.toString().trim();
    }

    private static double getCellNumeric(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) return 0;
        if (cell.getCellType() == CellType.NUMERIC) return cell.getNumericCellValue();
        try {
            return Double.parseDouble(cell.toString().trim());
        } catch (Exception e) {
            return 0;
        }
    }
}
