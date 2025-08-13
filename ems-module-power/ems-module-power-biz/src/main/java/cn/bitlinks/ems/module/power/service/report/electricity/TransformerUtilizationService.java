package cn.bitlinks.ems.module.power.service.report.electricity;

import cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo.TransformerUtilizationSettingsOptionsVO;
import cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo.TransformerUtilizationSettingsVO;
import cn.bitlinks.ems.module.power.controller.admin.report.hvac.vo.BaseReportResultVO;
import cn.bitlinks.ems.module.power.controller.admin.report.hvac.vo.TransformerUtilizationInfo;
import cn.bitlinks.ems.module.power.controller.admin.report.hvac.vo.TransformerUtilizationParamVO;

import javax.validation.Valid;
import java.util.List;

/**
 * 变压器利用率
 */
public interface TransformerUtilizationService {

    /**
     * 更新设置
     *
     * @param settings 设置内容
     */
    void updSettings(List<TransformerUtilizationSettingsVO> settings);

    /**
     * 查询设置
     *
     * @return list
     */
    List<TransformerUtilizationSettingsVO> getSettings();

    /**
     * 变压器下拉
     *
     * @return list
     */
    List<TransformerUtilizationSettingsOptionsVO> transformerOptions();


    BaseReportResultVO<TransformerUtilizationInfo> getTable(TransformerUtilizationParamVO paramVO);

    List<List<String>> getExcelHeader(@Valid TransformerUtilizationParamVO paramVO);

    List<List<Object>> getExcelData(@Valid TransformerUtilizationParamVO paramVO);
}
