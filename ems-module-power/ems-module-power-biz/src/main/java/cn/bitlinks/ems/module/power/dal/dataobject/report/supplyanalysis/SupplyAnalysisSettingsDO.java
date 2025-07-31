package cn.bitlinks.ems.module.power.dal.dataobject.report.supplyanalysis;

import cn.bitlinks.ems.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 供应分设置析 DO
 *
 * @author bitlinks
 */
@TableName("power_supply_analysis_settings")
@KeySequence("power_supply_analysis_settings_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplyAnalysisSettingsDO extends BaseDO {

    /**
     * id
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    /**
     * 系统
     */
    private String system;

    /**
     * 分析项
     */
    private String item;
    /**
     * 台账id
     */
    private Long standingbookId;


}