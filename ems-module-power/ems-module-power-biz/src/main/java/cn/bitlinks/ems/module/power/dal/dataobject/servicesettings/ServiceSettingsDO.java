package cn.bitlinks.ems.module.power.dal.dataobject.servicesettings;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.bitlinks.ems.framework.mybatis.core.dataobject.BaseDO;

/**
 * 服务设置 DO
 *
 * @author bitlinks
 */
@TableName("power_service_settings")
@KeySequence("power_service_settings_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceSettingsDO extends BaseDO {

    /**
     * id
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    /**
     * 服务名称
     */
    private String serviceName;
    /**
     * 协议类型(0：OPC-DA 1:MODBUS-TCP)
     */
    private Integer protocol;
    /**
     * IP地址
     */
    private String ipAddress;
    /**
     * 端口
     */
    private Integer port;
    /**
     * 重试次数，默认3
     */
    private Integer retryCount;
    /**
     * 注册表ID
     */
    private String clsid;
    /**
     * 用户名
     */
    private String username;
    /**
     * 密码
     */
    private String password;

}