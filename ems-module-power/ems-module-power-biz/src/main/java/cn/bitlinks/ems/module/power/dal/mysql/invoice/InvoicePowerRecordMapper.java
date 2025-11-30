package cn.bitlinks.ems.module.power.dal.mysql.invoice;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.invoice.vo.InvoicePowerRecordPageReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.invoice.InvoicePowerRecordDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface InvoicePowerRecordMapper extends BaseMapperX<InvoicePowerRecordDO> {

    default PageResult<InvoicePowerRecordDO> selectPage(InvoicePowerRecordPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<InvoicePowerRecordDO>()
                .betweenIfPresent(InvoicePowerRecordDO::getRecordMonth, reqVO.getRecordMonth())
                .orderByAsc(InvoicePowerRecordDO::getRecordMonth));
    }
}
