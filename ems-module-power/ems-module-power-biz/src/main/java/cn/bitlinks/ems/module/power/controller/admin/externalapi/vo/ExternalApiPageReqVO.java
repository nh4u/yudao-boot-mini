package cn.bitlinks.ems.module.power.controller.admin.externalapi.vo;

import cn.bitlinks.ems.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * @author liumingqiang
 */
@Schema(description = "管理后台 - 外部数据接口管理分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ExternalApiPageReqVO extends PageParam {

    @Schema(description = "接口名称")
    private String name;


}