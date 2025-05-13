package cn.bitlinks.ems.module.power.controller.admin.unitpriceconfiguration.vo;

import lombok.Data;

import java.util.List;

@Data
public class PriceDetail {
    private String max;
    private String min;
    private List<String> time;
    private String price;
    private String bucketType;
}
