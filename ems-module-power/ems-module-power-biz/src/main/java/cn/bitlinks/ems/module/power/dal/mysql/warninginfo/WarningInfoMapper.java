package cn.bitlinks.ems.module.power.dal.mysql.warninginfo;


import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.warninginfo.vo.WarningInfoLatestStrategyRespVO;
import cn.bitlinks.ems.module.power.controller.admin.warninginfo.vo.WarningInfoPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.warninginfo.vo.WarningInfoStatisticsRespVO;
import cn.bitlinks.ems.module.power.dal.dataobject.warninginfo.WarningInfoDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.framework.web.core.util.WebFrameworkUtils.getLoginUserId;


/**
 * 告警信息 Mapper
 *
 * @author bitlinks
 */
@Mapper
public interface WarningInfoMapper extends BaseMapperX<WarningInfoDO> {

    default PageResult<WarningInfoDO> selectPage(WarningInfoPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<WarningInfoDO>()
                .eqIfPresent(WarningInfoDO::getLevel, reqVO.getLevel())
                .betweenIfPresent(WarningInfoDO::getWarningTime, reqVO.getWarningTime())
                .eqIfPresent(WarningInfoDO::getStatus, reqVO.getStatus())
                .likeIfPresent(WarningInfoDO::getDeviceRel, reqVO.getDeviceRel())
                .eq(WarningInfoDO::getUserId, getLoginUserId())
                //.betweenIfPresent(WarningInfoDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(WarningInfoDO::getWarningTime));
    }
//    @Select("SELECT " +
//            "COUNT(1) AS total, " +
//            "SUM(CASE WHEN wi.level = 0 THEN 1 ELSE 0 END) AS count0, " +
//            "SUM(CASE WHEN wi.level = 1 THEN 1 ELSE 0 END) AS count1, " +
//            "SUM(CASE WHEN wi.level = 2 THEN 1 ELSE 0 END) AS count2, " +
//            "SUM(CASE WHEN wi.level = 3 THEN 1 ELSE 0 END) AS count3, " +
//            "SUM(CASE WHEN wi.level = 4 THEN 1 ELSE 0 END) AS count4 " +
//            "FROM power_warning_info wi " +
//            "INNER JOIN power_warning_info_user wiu ON wi.id = wiu.info_id " +
//            "WHERE wiu.user_id = #{userId}")
//    WarningInfoStatisticsRespVO countWarningsByLevel(@Param("userId") long userId);
    @Select("SELECT " +
        "COUNT(1) AS total, " +
        "COALESCE(SUM(CASE WHEN level = 0 THEN 1 ELSE 0 END), 0) AS count0, " +
        "COALESCE(SUM(CASE WHEN level = 1 THEN 1 ELSE 0 END), 0) AS count1, " +
        "COALESCE(SUM(CASE WHEN level = 2 THEN 1 ELSE 0 END), 0) AS count2, " +
        "COALESCE(SUM(CASE WHEN level = 3 THEN 1 ELSE 0 END), 0) AS count3, " +
        "COALESCE(SUM(CASE WHEN level = 4 THEN 1 ELSE 0 END), 0) AS count4 " +
        "FROM power_warning_info where user_id = #{userId} and deleted=0")
    WarningInfoStatisticsRespVO countWarningsByLevel(@Param("userId") long userId);

//    default void updateStatusById(Long id, Integer status) {
//        update(WarningInfoDO.builder().status(status).build(), new LambdaUpdateWrapper<WarningInfoDO>().eq(WarningInfoDO::getId, id));
//    }

    /**
     * 获取每个策略ID对应的最新告警信息的时间
     *
     * @return 一个Map，Key是策略ID，Value是最新告警时间
     */
    default Map<Long, LocalDateTime> selectLatestByStrategy() {
        List<WarningInfoLatestStrategyRespVO> latestWarnings = selectLatestCreateTimeByStrategy();

        return latestWarnings.stream()
                .collect(Collectors.toMap(
                        WarningInfoLatestStrategyRespVO::getStrategyId,
                        WarningInfoLatestStrategyRespVO::getTriggerTime
                ));
    }

    List<WarningInfoLatestStrategyRespVO> selectLatestCreateTimeByStrategy();
}