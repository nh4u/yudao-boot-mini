package cn.bitlinks.ems.module.power.enums;

public enum ChartSeriesTypeEnum {
    BAR("bar"),
    LINE("line");

    private final String type;

    ChartSeriesTypeEnum(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
