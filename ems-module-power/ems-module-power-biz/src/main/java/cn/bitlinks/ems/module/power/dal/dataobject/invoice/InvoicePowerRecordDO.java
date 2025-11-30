package cn.bitlinks.ems.module.power.dal.dataobject.invoice;

import cn.bitlinks.ems.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 发票电量记录（按月）DO
 */
@TableName("ems_invoice_power_record")
@KeySequence("ems_invoice_power_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoicePowerRecordDO extends BaseDO {

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 补录月份（建议前端传当月第一天，例如 2025-09-01）
     */
    private LocalDate recordMonth;

    /**
     * 金额(含税13%)，可为空
     */
    private BigDecimal amount;

    /**
     * 备注，可为空
     */
    private String remark;
}
