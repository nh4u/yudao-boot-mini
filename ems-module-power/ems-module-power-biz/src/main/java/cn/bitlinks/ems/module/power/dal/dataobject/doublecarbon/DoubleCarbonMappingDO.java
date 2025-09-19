package cn.bitlinks.ems.module.power.dal.dataobject.doublecarbon;

import cn.bitlinks.ems.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;


@TableName(value = "power_double_carbon_mapping", autoResultMap = true)
@KeySequence("power_double_carbon_mapping_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoubleCarbonMappingDO extends BaseDO {

    /**
     * id
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    /**
     * 台账编码
     */
    private String standingbookCode;
    /**
     * 双碳编码
     */
    private String doubleCarbonCode;
    /**
     * 台账id
     */
    private  Long  standingbookId;

}