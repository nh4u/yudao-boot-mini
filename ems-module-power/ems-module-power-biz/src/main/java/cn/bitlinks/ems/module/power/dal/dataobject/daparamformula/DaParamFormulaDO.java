package cn.bitlinks.ems.module.power.dal.dataobject.daparamformula;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.bitlinks.ems.framework.mybatis.core.dataobject.BaseDO;

/**
 * 数据来源为关联计量器具时的参数公式 DO
 *
 * @author Mingdy
 */
@TableName("ems_da_param_formula")
@KeySequence("ems_da_param_formula_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DaParamFormulaDO extends BaseDO {

    /**
     * id
     */
    @TableId
    private Long id;
    /**
     * 台账id
     */
    private Long standingBookId;
    /**
     * 能源参数名称
     */
    private String energyParam;
    /**
     * 能源参数计算公式
     */
    private String energyFormula;

}