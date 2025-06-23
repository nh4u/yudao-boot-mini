package cn.bitlinks.ems.module.power.dal.dataobject.standingbook.reportcod;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import cn.bitlinks.ems.framework.mybatis.core.dataobject.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 台账分类的数采参数表（自定义和能源） DO
 *
 * @author bitlinks
 */
@TableName("ems_header_code_mapping")
@KeySequence("ems_header_code_mapping_seq")
// 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeaderCodeMappingDO extends BaseDO {

    /**
     * 分类数采参数属性id
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    /**
     * 表头code
     */
    private String headerCode;
    /**
     * 表头
     */
    private String header;
    /**
     * 系统台账code
     */
    private String code;
    /**
     * 类型0：去空串完全匹配；1：去空串首部匹配；2：去空串尾部匹配；5：去尾部-完全匹配；6：去尾部-首部匹配；7：去尾部-尾部匹配；8：未匹配到。
     */
    private String type;

}