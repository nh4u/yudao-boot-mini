package cn.bitlinks.ems.module.power.controller.admin.deviceassociationconfiguration.vo;

import cn.bitlinks.ems.module.power.dal.dataobject.deviceassociationconfiguration.DeviceAssociationConfigurationDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
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

    private  Long standingbookTypeId;
    private  String standingbookTypeName;
    private  Long standingbookId;
    private  String standingbookName;
    private StandingbookDO standingbook;

    private List<AssociationData> children;

    private  Long deviceId;
    private  String deviceName;
    private  String deviceCode;

}

