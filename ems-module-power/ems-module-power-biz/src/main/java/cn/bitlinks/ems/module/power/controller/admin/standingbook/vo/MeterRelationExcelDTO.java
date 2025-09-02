package cn.bitlinks.ems.module.power.controller.admin.standingbook.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 计量器具关联关系 Excel 导入模型
 * 注：@ExcelProperty 的 value 必须与模板列名完全一致，修改列名会导致导入失败
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeterRelationExcelDTO {

    /**
     * 计量器具编号（必填项）
     * 模板列名：必须为“计量器具编号”
     */
    @ExcelProperty(index = 0) // index 确保列顺序匹配
    private String meterCode;

    /**
     * 下级计量器具编号（非必填，多个用英文;分隔）
     * 模板列名：必须为“下级计量器具编号”
     */
    @ExcelProperty(index = 1)
    private String subMeterCodes;

    /**
     * 关联设备（非必填）
     * 模板列名：必须为“关联设备”
     */
    @ExcelProperty(index = 2)
    private String relatedDevice;

    /**
     * 环节（非必填）
     * 模板列名：必须为“环节”
     */
    @ExcelProperty(index = 3)
    private String stage;

    /**
     * 行号（EasyExcel 解析时自动填充，用于错误提示）
     */
    private Integer rowNum;

}