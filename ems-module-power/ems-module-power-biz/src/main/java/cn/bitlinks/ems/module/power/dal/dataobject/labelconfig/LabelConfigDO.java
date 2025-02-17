package cn.bitlinks.ems.module.power.dal.dataobject.labelconfig;

import cn.bitlinks.ems.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 配置标签 DO
 *
 * @author bitlinks
 */
@TableName("ems_label_config")
@KeySequence("ems_label_config_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabelConfigDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 标签名称
     */
    private String labelName;
    /**
     * 排序
     */
    private Integer sort;
    /**
     * 备注
     */
    private String remark;
    /**
     * 编码
     */
    private String code;
    /**
     * 父标签ID
     */
    private Long parentId;
    /**
     * 是否为默认标签
     */
    private Boolean ifDefault;

}