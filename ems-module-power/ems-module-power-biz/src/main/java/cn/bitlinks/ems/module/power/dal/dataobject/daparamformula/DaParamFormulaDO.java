package cn.bitlinks.ems.module.power.dal.dataobject.daparamformula;

import cn.bitlinks.ems.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 数据来源为关联计量器具时的参数公式历史记录 DO
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
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    /**
     * 能源id
     */
    private Long energyId;
    /**
     * 能源参数名称
     */
    private String energyParam;
    /**
     * 能源参数计算公式
     */
    private String energyFormula;
    /**
     * 公式类型
     */
    private Integer formulaType;
    /**
     * 公式小数点
     */
    private Integer formulaScale;
    /**
     * 生效时间
     */
    private String effectiveTime;
}