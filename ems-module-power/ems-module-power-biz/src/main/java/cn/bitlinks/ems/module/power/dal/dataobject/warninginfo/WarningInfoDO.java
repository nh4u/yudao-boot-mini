package cn.bitlinks.ems.module.power.dal.dataobject.warninginfo;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.bitlinks.ems.framework.mybatis.core.dataobject.BaseDO;

/**
 * 告警信息 DO
 *
 * @author bitlinks
 */
@TableName("power_warning_info")
@KeySequence("power_warning_info_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarningInfoDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 告警等级：紧急4 重要3 次要2 警告1 提示0
     */
    private Integer level;
    /**
     * 用户id
     */
    private Long userId;
    /**
     * 告警时间
     */
    private LocalDateTime warningTime;
    /**
     * 处理状态:0-未处理1-处理中2-已处理
     */
    private Integer status;
    /**
     * 设备名称与编号
     */
    private String deviceRel;
    /**
     * 模板id
     */
    private Long templateId;
    /**
     * 标题
     */
    private String title;
    /**
     * 告警内容
     */
    private String content;

}