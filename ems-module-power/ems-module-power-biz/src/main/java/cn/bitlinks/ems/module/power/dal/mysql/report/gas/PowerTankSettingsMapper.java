package cn.bitlinks.ems.module.power.dal.mysql.report.gas;

import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.dal.dataobject.report.gas.PowerTankSettingsDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 储罐液位设置 Mapper
 *
 * @author bmqi
 */
@Mapper
public interface PowerTankSettingsMapper extends BaseMapperX<PowerTankSettingsDO> {

    default List<PowerTankSettingsDO> selectList() {
        return selectList(new LambdaQueryWrapperX<PowerTankSettingsDO>()
                .orderByDesc(PowerTankSettingsDO::getSortNo));
    }

    default Boolean savePowerTankSettings(List<PowerTankSettingsDO> powerTankSettingsDOList) {
        return updateBatch(powerTankSettingsDOList);
    }

}