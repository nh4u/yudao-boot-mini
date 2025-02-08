package cn.bitlinks.ems.module.power.dal.mysql.voucher;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.voucher.vo.VoucherPageReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.voucher.VoucherDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
                .likeIfPresent(VoucherDO::getEnergyName, reqVO.getEnergyName())
                .betweenIfPresent(VoucherDO::getPurchaseTime, reqVO.getPurchaseTime())
                .eqIfPresent(VoucherDO::getPrice, reqVO.getPrice())
                .eqIfPresent(VoucherDO::getUsage, reqVO.getUsage())
                .betweenIfPresent(VoucherDO::getUpdateTime, reqVO.getUpdateTime())
                .orderByDesc(VoucherDO::getId));
    }

    // 新增方法用于获取当日最大流水号
    @Select("SELECT MAX(CAST(SUBSTRING(code, LENGTH(#{codePrefix}) + 1) AS UNSIGNED)) " +
            "FROM ems_voucher WHERE code LIKE CONCAT(#{codePrefix}, '%')")
    Integer selectMaxSerialByCodePrefix(@Param("codePrefix") String codePrefix);
}
