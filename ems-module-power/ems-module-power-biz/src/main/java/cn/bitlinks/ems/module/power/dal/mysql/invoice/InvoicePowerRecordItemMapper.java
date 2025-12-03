package cn.bitlinks.ems.module.power.dal.mysql.invoice;

import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.module.power.dal.dataobject.invoice.InvoicePowerRecordItemDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface InvoicePowerRecordItemMapper extends BaseMapperX<InvoicePowerRecordItemDO> {

    default List<InvoicePowerRecordItemDO> selectListByRecordId(Long recordId) {
        return selectList(InvoicePowerRecordItemDO::getRecordId, recordId);
    }

    default List<InvoicePowerRecordItemDO> selectListByRecordIds(Collection<Long> recordIds) {
        return selectList(InvoicePowerRecordItemDO::getRecordId, recordIds);
    }

    default int deleteByRecordId(Long recordId) {
        return delete(InvoicePowerRecordItemDO::getRecordId, recordId);
    }
}
