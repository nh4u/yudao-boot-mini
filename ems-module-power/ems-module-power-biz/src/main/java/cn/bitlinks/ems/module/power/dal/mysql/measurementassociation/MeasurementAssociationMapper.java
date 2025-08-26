package cn.bitlinks.ems.module.power.dal.mysql.measurementassociation;

import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.module.power.dal.dataobject.measurementassociation.MeasurementAssociationDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 计量器具下级计量配置 Mapper
 *
 * @author bitlinks
 */
@Mapper
public interface MeasurementAssociationMapper extends BaseMapperX<MeasurementAssociationDO> {
    /**
     * 获取所有叶节点的测量ID
     *
     * @param sbIds 设备ID列表，用于查询相关的叶节点测量ID
     * @return 返回所有叶节点的测量ID列表
     */
    List<Long> getNotLeafMeasurementId(@Param("sbIds") List<Long> sbIds);

/**
 * 获取非顶层的测量ID列表
 *
 * @param stageSbIds 阶段SB ID列表，用于筛选非顶层的测量ID
 * @return 返回非顶层的测量ID列表，类型为Long列表
 */
    List<Long> getNotToppestMeasurementId(@Param("sbIds")List<Long> stageSbIds);
}