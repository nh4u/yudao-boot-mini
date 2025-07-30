package cn.bitlinks.ems.module.power.dal.mysql.minuteagg;

import cn.bitlinks.ems.module.power.controller.admin.report.hvac.vo.BaseTimeDateParamVO;
import cn.bitlinks.ems.module.power.dal.dataobject.minuteagg.MinuteAggregateDataDO;
import org.apache.ibatis.annotations.Mapper;

import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 分钟聚合数据mapper
 */
@Mapper
public interface MinuteAggregateDataMapper {

    /**
     * 查询用量
     * @param paramVO
     * @param localDateTime
     * @param localDateTime1
     * @param singleton
     * @return
     */
    List<MinuteAggregateDataDO> getList(BaseTimeDateParamVO paramVO, @Size(min = 2, max = 2, message = "统计周期不能为空") LocalDateTime localDateTime, @Size(min = 2, max = 2, message = "统计周期不能为空") LocalDateTime localDateTime1, Set<Long> singleton);
}

