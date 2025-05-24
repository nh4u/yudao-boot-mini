package cn.bitlinks.ems.module.acquisition.api.starrocks.dto;

import lombok.Data;

import java.util.List;

@Data
public class StreamLoadDTO {
    /**
     * 数据
     */
    private List<?> data;
    /**
     * 标签
     */
    private String label;
    /**
     * 表名
     */
    private String tableName;
}
