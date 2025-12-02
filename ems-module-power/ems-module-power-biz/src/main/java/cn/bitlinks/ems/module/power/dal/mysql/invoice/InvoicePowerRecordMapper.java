package cn.bitlinks.ems.module.power.dal.mysql.invoice;

import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.invoice.vo.InvoicePowerRecordPageReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.invoice.InvoicePowerRecordDO;
import cn.bitlinks.ems.module.power.dal.dataobject.invoice.InvoicePowerRecordItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface InvoicePowerRecordMapper extends BaseMapperX<InvoicePowerRecordDO> {

    default List<InvoicePowerRecordDO> selectList(InvoicePowerRecordPageReqVO reqVO) {
        return selectList(new LambdaQueryWrapperX<InvoicePowerRecordDO>()
                // 补录月份区间 [开始, 结束]
                .betweenIfPresent(InvoicePowerRecordDO::getRecordMonth, reqVO.getRecordMonth())
                .orderByDesc(InvoicePowerRecordDO::getRecordMonth)
        );
    }

    default InvoicePowerRecordDO selectByRecordMonth(LocalDate recordMonth) {
        return selectOne(new LambdaQueryWrapperX<InvoicePowerRecordDO>()
                .eq(InvoicePowerRecordDO::getRecordMonth, recordMonth)
                .last("LIMIT 1"));
    }

}
