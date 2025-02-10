package cn.bitlinks.ems.module.power.dal.mysql.unitpricehistory;

import java.time.LocalDateTime;
import java.util.*;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.module.power.dal.dataobject.coalfactorhistory.CoalFactorHistoryDO;
import cn.bitlinks.ems.module.power.dal.dataobject.unitpricehistory.UnitPriceHistoryDO;
import org.apache.ibatis.annotations.Mapper;
import cn.bitlinks.ems.module.power.controller.admin.unitpricehistory.vo.*;

/**
 * 单价历史 Mapper
 *
 * @author bitlinks
 */
@Mapper
public interface UnitPriceHistoryMapper extends BaseMapperX<UnitPriceHistoryDO> {

    default PageResult<UnitPriceHistoryDO> selectPage(UnitPriceHistoryPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<UnitPriceHistoryDO>()
                .eqIfPresent(UnitPriceHistoryDO::getEnergyId, reqVO.getEnergyId())
                .betweenIfPresent(UnitPriceHistoryDO::getStartTime, reqVO.getStartTime())
                .betweenIfPresent(UnitPriceHistoryDO::getEndTime, reqVO.getEndTime())
                .eqIfPresent(UnitPriceHistoryDO::getBillingMethod, reqVO.getBillingMethod())
                .eqIfPresent(UnitPriceHistoryDO::getAccountingFrequency, reqVO.getAccountingFrequency())
                .eqIfPresent(UnitPriceHistoryDO::getPriceDetails, reqVO.getPriceDetails())
                .eqIfPresent(UnitPriceHistoryDO::getFormula, reqVO.getFormula())
                .eqIfPresent(UnitPriceHistoryDO::getUpdater,reqVO.getUpdater())
                .betweenIfPresent(UnitPriceHistoryDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(UnitPriceHistoryDO::getId));
    }

}