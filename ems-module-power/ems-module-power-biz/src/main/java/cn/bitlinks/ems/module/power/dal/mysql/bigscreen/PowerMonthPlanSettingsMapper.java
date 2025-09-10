package cn.bitlinks.ems.module.power.dal.mysql.bigscreen;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.bigscreen.vo.PowerMonthPlanSettingsPageReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.bigscreen.PowerMonthPlanSettingsDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author liumingqiang
 */
@Mapper
public interface PowerMonthPlanSettingsMapper extends BaseMapperX<PowerMonthPlanSettingsDO> {

    default PageResult<PowerMonthPlanSettingsDO> selectPage(PowerMonthPlanSettingsPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<PowerMonthPlanSettingsDO>()
                .orderByAsc(PowerMonthPlanSettingsDO::getId));
    }

}