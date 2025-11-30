package cn.bitlinks.ems.module.power.dal.dataobject.invoice;

import cn.bitlinks.ems.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * 发票电量记录明细（按表计）DO
 */
@TableName("ems_invoice_power_record_item")
@KeySequence("ems_invoice_power_record_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoicePowerRecordItemDO extends BaseDO {

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 主表ID
     */
    private Long recordId;

    /**
     * 表计编号（字典：invoice_meter_code）
     */
    private String meterCode;

    /**
     * 总电度(kWh)，可为空
     */
    private BigDecimal totalKwh;

    /**
     * 需量电度(kWh)，可为空
     */
    private BigDecimal demandKwh;
}
