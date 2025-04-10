package cn.bitlinks.ems.module.power.dal.mysql.pricedetail;

import java.util.*;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.module.power.dal.dataobject.pricedetail.PriceDetailDO;
import org.apache.ibatis.annotations.Mapper;
import cn.bitlinks.ems.module.power.controller.admin.pricedetail.vo.*;
import org.apache.ibatis.annotations.Param;

/**
 * 单价详细 Mapper
 *
 * @author bitlinks
 */
@Mapper
public interface PriceDetailMapper extends BaseMapperX<PriceDetailDO> {

    default PageResult<PriceDetailDO> selectPage(PriceDetailPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<PriceDetailDO>()
                .eqIfPresent(PriceDetailDO::getPriceId, reqVO.getPriceId())
                .eqIfPresent(PriceDetailDO::getPeriodType, reqVO.getPeriodType())
                .eqIfPresent(PriceDetailDO::getPeriodStart, reqVO.getPeriodStart())
                .eqIfPresent(PriceDetailDO::getPeriodEnd, reqVO.getPeriodEnd())
                .eqIfPresent(PriceDetailDO::getUsageMin, reqVO.getUsageMin())
                .eqIfPresent(PriceDetailDO::getUsageMax, reqVO.getUsageMax())
                .eqIfPresent(PriceDetailDO::getUnitPrice, reqVO.getUnitPrice())
                .betweenIfPresent(PriceDetailDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(PriceDetailDO::getId));
    }

    List<PriceDetailDO> selectByPriceId(@Param("priceId") Long priceId);

    List<PriceDetailDO> selectByPriceIds(@Param("priceIds") List<Long> priceIds);

}