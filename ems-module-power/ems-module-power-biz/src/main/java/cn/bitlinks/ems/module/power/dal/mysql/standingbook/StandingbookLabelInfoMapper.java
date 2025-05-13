package cn.bitlinks.ems.module.power.dal.mysql.standingbook;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookLabelInfoDO;

/**
 * 台账标签 Mapper
 *
 * @author bitlinks
 */
@Mapper
public interface StandingbookLabelInfoMapper extends BaseMapperX<StandingbookLabelInfoDO> {

    /**
     * 根据标签查询台账id
     *
     * @param labelInfoConditions 标签条件参数列表
     * @return sbId
     */
    List<Long> selectStandingbookIdByLabelCondition(@Param("labelInfoConditions") Map<String, List<String>> labelInfoConditions, @Param("sbIds") List<Long> sbIds);


    default List<StandingbookLabelInfoDO> getByLabelNames(List<String> labelNames) {
        LambdaQueryWrapper<StandingbookLabelInfoDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(StandingbookLabelInfoDO::getName, labelNames);
        return selectList(wrapper);
    }

     List<StandingbookLabelInfoDO> getByValues(@Param("valueList") List<String> values);
}