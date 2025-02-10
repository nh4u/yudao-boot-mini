package cn.bitlinks.ems.module.power.dal.dataobject.coalfactorhistory;

import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.bitlinks.ems.framework.mybatis.core.dataobject.BaseDO;

/**
 * 折标煤系数历史 DO
 *
 * @author bitlinks
 */
@TableName("ems_coal_factor_history")
@KeySequence("ems_coal_factor_history_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoalFactorHistoryDO extends BaseDO {

    /**
     * id
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    /**
     * 能源id
     */
    private Long energyId;
    /**
     * 生效开始时间
     */
    private LocalDateTime startTime;
    /**
     * 生效结束时间
     */
    private LocalDateTime endTime;
    /**
     * 折标煤系数
     */
    private BigDecimal factor;
    /**
     * 关联计算公式
     */
    private String formula;

}