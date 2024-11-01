package cn.bitlinks.ems.module.power.dal.mysql.voucher;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.voucher.vo.VoucherPageReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.voucher.VoucherDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 凭证管理 Mapper
 *
 * @author 张亦涵
 */
@Mapper
public interface VoucherMapper extends BaseMapperX<VoucherDO> {

    default PageResult<VoucherDO> selectPage(VoucherPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<VoucherDO>()
                .eqIfPresent(VoucherDO::getCode, reqVO.getCode())
                .likeIfPresent(VoucherDO::getName, reqVO.getName())
                .eqIfPresent(VoucherDO::getEnergyId, reqVO.getEnergyId())
                .betweenIfPresent(VoucherDO::getPurchaseTime, reqVO.getPurchaseTime())
                .eqIfPresent(VoucherDO::getPrice, reqVO.getPrice())
                .eqIfPresent(VoucherDO::getUsage, reqVO.getUsage())
                .betweenIfPresent(VoucherDO::getUpdateTime, reqVO.getUpdateTime())
                .orderByDesc(VoucherDO::getId));
    }

}