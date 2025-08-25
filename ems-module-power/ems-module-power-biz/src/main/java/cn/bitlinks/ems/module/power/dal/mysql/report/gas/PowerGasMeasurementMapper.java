package cn.bitlinks.ems.module.power.dal.mysql.report.gas;

import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.dal.dataobject.report.gas.PowerGasMeasurementDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 气化科固定43条计量器具配置 Mapper
 *
 * @author bmqi
 */
@Mapper
public interface PowerGasMeasurementMapper extends BaseMapperX<PowerGasMeasurementDO> {

    /**
     * 获取所有有效的计量器具配置，按排序号排序
     */
    default List<PowerGasMeasurementDO> selectAllValid() {
        return selectList(new LambdaQueryWrapperX<PowerGasMeasurementDO>()
                .eq(PowerGasMeasurementDO::getDeleted, false)
                .orderByAsc(PowerGasMeasurementDO::getSortNo));
    }

    /**
     * 根据计量器具编号列表获取配置
     */
    default List<PowerGasMeasurementDO> selectByMeasurementCodes(List<String> measurementCodes) {
        return selectList(new LambdaQueryWrapperX<PowerGasMeasurementDO>()
                .in(PowerGasMeasurementDO::getMeasurementCode, measurementCodes)
                .eq(PowerGasMeasurementDO::getDeleted, false)
                .orderByAsc(PowerGasMeasurementDO::getSortNo));
    }
}
