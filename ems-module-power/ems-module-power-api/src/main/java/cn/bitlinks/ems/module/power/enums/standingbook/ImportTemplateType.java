package cn.bitlinks.ems.module.power.enums.standingbook;

import java.util.Arrays;
import java.util.List;

public enum ImportTemplateType {
    EQUIPMENT("重点设备台账",
            Arrays.asList("*设备分类", "*设备名称", "*设备编号") // 固定表头前缀
    ),
    METER("计量器具台账",
            Arrays.asList("*设备分类", "表类型", "*设备名称", "*设备编号")
    );

    private final String name;
    private final List<String> baseHeaders;

    ImportTemplateType(String name, List<String> baseHeaders) {
        this.name = name;
        this.baseHeaders = baseHeaders;
    }

    public List<String> getBaseHeaders() {
        return baseHeaders;
    }

}
