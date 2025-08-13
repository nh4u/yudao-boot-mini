package cn.bitlinks.ems.module.power.service.report.gas;

import cn.bitlinks.ems.module.power.controller.admin.report.gas.vo.GasMeasurementInfo;
import cn.bitlinks.ems.module.power.dal.dataobject.report.gas.PowerGasMeasurementDO;

import java.util.List;

/**
 * 气化科固定43条计量器具配置 Service 接口
 *
 * @author bmqi
 */
public interface PowerGasMeasurementService {

    /**
     * 获取所有有效的计量器具配置
     */
    List<PowerGasMeasurementDO> getAllValidMeasurements();

    /**
     * 根据计量器具编号列表获取配置
     */
    List<PowerGasMeasurementDO> getMeasurementsByCodes(List<String> measurementCodes);

    /**
     * 获取计量器具的台账ID和参数编码映射
     * 根据measurementCode查询台账信息，根据energyParam查询能源参数code
     */
    List<GasMeasurementInfo> getGasMeasurementInfos();
}
