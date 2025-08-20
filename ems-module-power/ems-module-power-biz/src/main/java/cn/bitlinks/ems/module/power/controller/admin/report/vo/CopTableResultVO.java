package cn.bitlinks.ems.module.power.controller.admin.report.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;


/**
 * @author liumingqiang
 */
@Schema(description = "管理后台 - cop统计结果 VO")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
public class CopTableResultVO<T> {

    /**
     * 统计信息
     */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private List<T> copMapList;

    /**
     * 数据最后更新时间
     */
    private LocalDateTime dataTime;


}
