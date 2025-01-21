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
    private StandingbookDO standingbook;
    private List<DeviceAssociationConfigurationDO> associations;
}