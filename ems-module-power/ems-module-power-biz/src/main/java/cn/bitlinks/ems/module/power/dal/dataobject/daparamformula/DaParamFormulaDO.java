package cn.bitlinks.ems.module.power.dal.dataobject.daparamformula;

import cn.bitlinks.ems.framework.mybatis.core.dataobject.BaseDO;
import cn.bitlinks.ems.module.power.controller.admin.energyparameters.vo.EnergyParametersSaveReqVO;
import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据来源为关联计量器具时的参数公式历史记录 DO
 *
 * @author Mingdy
 */
@TableName(value = "ems_da_param_formula", autoResultMap = true)
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
     * 公式状态【0:未使用；1：使用中；2：已使用】
     */
    private Integer formulaStatus;
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
     * 开始生效时间
     */
    private LocalDateTime startEffectiveTime;

    /**
     * 结束生效时间
     */
    private LocalDateTime endEffectiveTime;
}