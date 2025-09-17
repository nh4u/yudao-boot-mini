package cn.bitlinks.ems.module.power.dal.dataobject.doublecarbon;

import cn.bitlinks.ems.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;


@TableName(value = "power_double_carbon_settings", autoResultMap = true)
@KeySequence("power_double_carbon_settings_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoubleCarbonSettingsDO extends BaseDO {

    /**
     * id
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String name;
    private String url;
    /**
     * 更新频率
     */
    private Long updateFrequency;
    /**
     * 更新频率 单位
     */
    private Integer updateFrequencyUnit;
    /**
     * 上次执行时间
     */
    private LocalDateTime lastSyncTime;

}