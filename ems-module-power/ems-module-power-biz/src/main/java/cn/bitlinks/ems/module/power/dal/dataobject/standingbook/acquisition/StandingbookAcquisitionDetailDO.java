package cn.bitlinks.ems.module.power.dal.dataobject.standingbook.acquisition;

import cn.bitlinks.ems.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 台账-数采设置-详细信息 DO
 *
 * @author bitlinks
 */
@TableName("power_standingbook_acquisition_detail")
@KeySequence("power_standingbook_acquisition_detail_seq")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StandingbookAcquisitionDetailDO extends BaseDO {

    /**
     * 编号
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    /**
     * 参数采集开关（0：关；1开。）
     */
    private Boolean status;
    /**
     * 数采设置id
     */
    private Long acquisitionId;
    /**
     * OPCDA：io地址/MODBUS：
     */
    private String dataSite;
    /**
     * 公式
     */
    private String formula;
    /**
     * 全量/增量（0：全量；1增量。）
     */
    private Integer fullIncrement;
    /**
     * 参数编码
     */
    private String code;
    /**
     * 是否能源数采参数 0自定义数采 1能源数采
     */
    private Boolean energyFlag;


}