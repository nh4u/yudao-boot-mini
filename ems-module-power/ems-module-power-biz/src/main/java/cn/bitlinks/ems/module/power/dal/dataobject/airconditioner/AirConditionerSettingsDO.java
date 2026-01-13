package cn.bitlinks.ems.module.power.dal.dataobject.airconditioner;

import cn.bitlinks.ems.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 空调工况报表设置表
 */
@TableName("air_conditioner_settings")
@KeySequence("air_conditioner_settings_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AirConditionerSettingsDO extends BaseDO {

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    /**
     * 统计项名称
     */
    private String itemName;
    /**
     * 数据点位名称
     */
    private String dataSiteName;
    /**
     * 数据点位
     */
    private String dataSite;
    /**
     * 连接服务ip
     */
    private String ip;
    private String user;
    private String password;
    private String clsid;

    /**
     * 排序
     */
    private Integer sortNo;

}
