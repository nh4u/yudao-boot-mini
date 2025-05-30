package cn.bitlinks.ems.module.power.dal.dataobject.standingbook;

import cn.bitlinks.ems.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 台账标签信息 DO
 *
 * @author bitlinks
 */
@TableName("power_standingbook_label_info")
@KeySequence("power_standingbook_label_info_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StandingbookLabelInfoDO extends BaseDO {

    /**
     * 编号
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    /**
     * 台账id
     */
    private Long standingbookId;
    /**
     * 标签key
     */
    private String name;
    /**
     * 标签值
     */
    @TableField(value = "value")
    private String value;

}