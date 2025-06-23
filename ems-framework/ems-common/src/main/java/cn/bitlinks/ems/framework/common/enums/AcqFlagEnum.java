package cn.bitlinks.ems.framework.common.enums;

import cn.bitlinks.ems.framework.common.core.IntArrayValuable;
import cn.hutool.core.util.ObjUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 业务点
 *
 * @author bitlinks
 */
@Getter
@AllArgsConstructor
public enum AcqFlagEnum {

    ACQ(1, "业务点"),
    NOT_ACQ(0, "非业务点");


    private final Integer code;

    private final String desc;



}
