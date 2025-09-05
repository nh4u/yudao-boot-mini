package cn.bitlinks.ems.module.power.dal.dataobject.warninginfo;

import cn.bitlinks.ems.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 告警信息-关联用户 DO
 *
 * @author bitlinks
 */
@TableName("power_warning_info_user")
@KeySequence("power_warning_info_user_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarningInfoUserDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;

    /**
     * 用户id
     */
    private Long userId;
    /**
     * 告警信息id
     */
    private Long infoId;

}