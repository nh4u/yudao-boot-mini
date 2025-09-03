package cn.bitlinks.ems.module.power.controller.admin.standingbook.vo;

import lombok.Data;

import java.util.List;

/**
 * @author liumingqiang
 */

@Data
public class StandingbookExportVO {

    /**
     * 表头
     */
    private List<List<String>> headerList;
    /**
     * 行数据
     */
    private List<List<Object>> dataList;
    /**
     * 文件名称
     */
    private String filename;
}
