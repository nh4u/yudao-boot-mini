package cn.bitlinks.ems.module.power.dal.mysql.bigscreen;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.report.supplywatertmp.vo.SupplyWaterTmpSettingsPageReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.bigscreen.PowerPureWasteWaterGasSettingsDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author liumingqiang
 */
@Mapper
public interface PowerPureWasteWaterGasSettingsMapper extends BaseMapperX<PowerPureWasteWaterGasSettingsDO> {

    default PageResult<PowerPureWasteWaterGasSettingsDO> selectPage(SupplyWaterTmpSettingsPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<PowerPureWasteWaterGasSettingsDO>()
                .eqIfPresent(PowerPureWasteWaterGasSettingsDO::getSystem, reqVO.getSystem())
                .eqIfPresent(PowerPureWasteWaterGasSettingsDO::getStandingbookIds, reqVO.getStandingbookId())
                .orderByAsc(PowerPureWasteWaterGasSettingsDO::getId));
    }

}