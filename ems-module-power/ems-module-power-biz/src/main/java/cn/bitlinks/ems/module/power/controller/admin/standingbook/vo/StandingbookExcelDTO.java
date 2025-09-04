package cn.bitlinks.ems.module.power.controller.admin.standingbook.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 台账与标签 Excel 导入模型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StandingbookExcelDTO {

    /**
     * *设备分类（必填项）
     * 模板列名：必须为“计量器具编号”
     */
    private String typeCode;

    /**
     * 表类型
     */
    private String tableType;

    /**
     * 设备名称
     */
    private String sbName;

    /**
     * 设备编号（必填）
     */
    private String sbCode;

    /**
     * 行号（EasyExcel 解析时自动填充，用于错误提示）
     */
    private Integer rowNum;
    private Map<String,String> labelMap;

}