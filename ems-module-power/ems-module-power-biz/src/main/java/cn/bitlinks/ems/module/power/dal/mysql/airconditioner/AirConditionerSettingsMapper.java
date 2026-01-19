package cn.bitlinks.ems.module.power.dal.mysql.airconditioner;

import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.module.power.dal.dataobject.airconditioner.AirConditionerSettingsDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AirConditionerSettingsMapper extends BaseMapperX<AirConditionerSettingsDO> {

    @Select("SELECT item_name FROM ems_air_conditioner_settings GROUP BY item_name ORDER BY MIN(sort_no) ASC")
    List<String> selectDistinctItemNames();
}
