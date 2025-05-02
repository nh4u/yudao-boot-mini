package cn.bitlinks.ems.module.power.controller.admin.deviceassociationconfiguration.vo;

import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookLabelInfoDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * @Title: identifier-carrier
 * @description:
 * @Author: Jiayun CUI
 * @Date 2025/01/21 11:06
 **/
@Data
public class StandingbookWithAssociations {

    @Schema(description = "计量器具类型id")
    private Long standingbookTypeId;
    @Schema(description = "计量器具类型名称")
    private String standingbookTypeName;
    @Schema(description = "计量器具id")
    private Long standingbookId;
    @Schema(description = "计量器具名称")
    private String standingbookName;
    @Schema(description = "计量器具编号")
    private String measuringInstrumentId;
    @Schema(description = "表类型")
    private String tableType;
    @Schema(description = "数值类型")
    private String valueType;
    @Schema(description = "环节")
    private Integer stage;
    @Schema(description = "标签信息")
    private List<StandingbookLabelInfoDO> labelInfo;

    private List<AssociationData> children;

    @Schema(description = "设备id")
    private Long deviceId;
    @Schema(description = "设备名字")
    private String deviceName;
    @Schema(description = "设备编码")
    private String deviceCode;

}

