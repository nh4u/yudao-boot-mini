package cn.bitlinks.ems.module.power.dal.dataobject.sharefile;

import cn.bitlinks.ems.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 服务设置 DO
 *
 * @author bitlinks
 */
@TableName("power_share_file_settings")
@KeySequence("power_share_file_settings_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareFileSettingsDO extends BaseDO {

    /**
     * id
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    /**
     * 部门名称
     */
    private String name;
    /**
     * 目录拼接类型[1：年月日；2：年。]
     */
    private Integer type;
    /**
     * IP地址
     */
    private String ip;
    /**
     * 共享文件夹地址前缀
     */
    private String dir;
}