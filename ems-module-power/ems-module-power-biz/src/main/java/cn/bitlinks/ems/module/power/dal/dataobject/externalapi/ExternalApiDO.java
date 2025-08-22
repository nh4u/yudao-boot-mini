package cn.bitlinks.ems.module.power.dal.dataobject.externalapi;

import cn.bitlinks.ems.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;


/**
 * @author liumingqiang
 */
@TableName("power_external_api")
@KeySequence("power_external_api_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalApiDO extends BaseDO {

    /**
     * 编号
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    /**
     * 接口名称
     */
    private String name;
    /**
     * 接口编码
     */
    private String code;

    /**
     * 接口地址
     */
    private String url;
    /**
     * 请求方式
     */
    private String method;
    /**
     * body
     */
    private String body;

}