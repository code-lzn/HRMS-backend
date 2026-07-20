package com.limou.hrms.utils;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * EasyExcel 工具类 —— 提供 Web 导出等通用方法
 *
 * @author lubo
 */
@Slf4j
public class ExcelUtils {

    /**
     * Web 导出 Excel（自动设置响应头 + 自适应列宽）
     *
     * @param response  HttpServletResponse
     * @param fileName  文件名（不含 .xlsx 扩展名）
     * @param sheetName sheet 名称
     * @param data      数据列表
     * @param clazz     数据类（字段需标注 {@code @ExcelProperty}）
     * @param <T>       数据类型
     */
    public static <T> void export(HttpServletResponse response,
                                   String fileName,
                                   String sheetName,
                                   List<T> data,
                                   Class<T> clazz) {
        try {
            setExcelResponseHeader(response, fileName);
            EasyExcel.write(response.getOutputStream(), clazz)
                    .sheet(sheetName)
                    .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                    .doWrite(data);
            log.info("Excel export success: fileName={}, rows={}", fileName, data.size());
        } catch (IOException e) {
            log.error("Excel export failed: fileName={}", fileName, e);
            throw new RuntimeException("Excel 导出失败", e);
        }
    }

    /**
     * 设置 Excel 文件下载响应头
     *
     * @param response HttpServletResponse
     * @param fileName 文件名（不含扩展名）
     */
    private static void setExcelResponseHeader(HttpServletResponse response, String fileName) {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        try {
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name())
                    .replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition",
                    "attachment;filename=" + encodedFileName + ".xlsx");
        } catch (IOException e) {
            // 降级处理：使用未编码文件名
            response.setHeader("Content-Disposition",
                    "attachment;filename=" + fileName + ".xlsx");
        }
    }
}
