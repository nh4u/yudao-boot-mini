package cn.bitlinks.ems.module.power.dal.mysql.chemicals;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.chemicals.vo.PowerChemicalsSettingsPageReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.chemicals.PowerChemicalsSettingsDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author liumingqiang
 */
@Mapper
public interface PowerChemicalsSettingsMapper extends BaseMapperX<PowerChemicalsSettingsDO> {

    default PageResult<PowerChemicalsSettingsDO> selectPage(PowerChemicalsSettingsPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<PowerChemicalsSettingsDO>()
                .eqIfPresent(PowerChemicalsSettingsDO::getCode, reqVO.getCode())
                .betweenIfPresent(PowerChemicalsSettingsDO::getTime, reqVO.getRange())
                .orderByAsc(PowerChemicalsSettingsDO::getId));
    }

}