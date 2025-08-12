package cn.bitlinks.ems.module.power.dal.mysql.report.electricity;

import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.module.power.dal.dataobject.report.electricity.TransformerUtilizationSettingsDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TransformerUtilizationSettingsMapper extends BaseMapperX<TransformerUtilizationSettingsDO> {
}
