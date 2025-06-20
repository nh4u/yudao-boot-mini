package cn.bitlinks.ems.module.power.dal.dataobject.copsettings;

import cn.bitlinks.ems.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * cop公式设置 DO
 *
 * @author bitlinks
 */
@TableName("power_cop_settings")
@KeySequence("power_cop_settings_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CopSettingsDO extends BaseDO {

    /**
     * id
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    /**
     * 低温冷机 LTC,低温系统 LTS,中温冷机 MTC,中温系统 MTS
     */
    private String copType;
    /**
     * 协议类型(0：OPC-DA 1:MODBUS-TCP)
     */
    private String param;
    /**
     * IP地址
     */
    private String ipAddress;
    /**
     * 台账id
     */
    private Long standingbookId;


}