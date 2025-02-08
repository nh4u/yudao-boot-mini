package cn.bitlinks.ems.module.power.dal.dataobject.voucher;

import cn.bitlinks.ems.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 凭证管理 DO
 *
 * @author 张亦涵
 */
@TableName("ems_voucher")
@KeySequence("ems_voucher_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherDO extends BaseDO {

    /**
     * 编号
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    /**
     * 凭证编号
     */
    private String code;
    /**
     * 凭证名称
     */
    private String name;
    /**
     * 能源id
     */
    private Long energyId;
    /**
     * 能源name
     */
    private String energyName;
    /**
     * 购入时间
     */
    private LocalDateTime purchaseTime;
    /**
     * 经办人
     */
    private String attention;
    /**
     * 金额
     */
    private BigDecimal price;
    /**
     * 用量
     */
    @TableField(value = "`usage`")
    private BigDecimal usage;
    /**
     * 用量单位
     */
    private String usageUnit;
    /**
     * 描述
     */
    private String description;
    /**
     * 附件名称
     */
    private String appendixName;
    /**
     * 附件地址
     */
    private String appendixUrl;
    /**
     * 识别结果
     */
    private String results;

}