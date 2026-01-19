package cn.bitlinks.ems.module.power.dal.mysql.airconditioner;

import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.module.power.dal.dataobject.airconditioner.AirConditionerStatusDataDO;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.apache.ibatis.annotations.Mapper;

@DS("starrocks")
@Mapper
public interface AirConditionerStatusDataMapper extends BaseMapperX<AirConditionerStatusDataDO> {


}