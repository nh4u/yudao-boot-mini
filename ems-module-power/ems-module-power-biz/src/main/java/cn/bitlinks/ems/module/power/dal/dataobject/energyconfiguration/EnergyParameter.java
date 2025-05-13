package cn.bitlinks.ems.module.power.dal.dataobject.energyconfiguration;

import lombok.Data;

/**
 * @Title: ydme-ems
 * @description:
 * @Author: Mingqiang LIU
 * @Date 2025/03/24 15:46
 **/

@Data
@Deprecated
public class EnergyParameter {

    private String code;
    private String unit;
    private String chinese;
    private String english;
}
