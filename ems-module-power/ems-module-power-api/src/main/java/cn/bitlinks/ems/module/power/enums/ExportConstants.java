package cn.bitlinks.ems.module.power.enums;

/**
 * @Title: ydme-ems
 * @description: 报表导出枚举类
 * @Author: Mingqiang LIU
 * @Date 2025/07/25 11:20
 **/
public interface ExportConstants {

    // 折标煤

    String STANDARD_COAL_ALL = "折标煤用量分析明细报表（整体）";
    String STANDARD_COAL_ENERGY = "折标煤用量分析明细报表（按能源）";
    String STANDARD_COAL_LABEL = "折标煤用量分析明细报表（按标签）";

    // 用能成本

    String COST_ALL = "折价分析明细报表（整体）";
    String COST_ENERGY = "折价分析明细报表（按能源）";
    String COST_LABEL = "折价分析明细报表（按标签）";

    // 同比-折标煤

    String STANDARD_COAL_YOY_ALL = "折标煤用量同比分析明细报表（整体）";
    String STANDARD_COAL_YOY_ENERGY = "折标煤用量同比分析明细报表（按能源）";
    String STANDARD_COAL_YOY_LABEL = "折标煤用量同比分析明细报表（按标签）";

    // 同比-用能成本

    String COST_YOY_ALL = "用能成本同比分析明细报表（整体）";
    String COST_YOY_ENERGY = "用能成本同比分析明细报表（按能源）";
    String COST_YOY_LABEL = "用能成本同比分析明细报表（按标签）";

    // 同比-利用率

    String USAGE_RATE_YOY_ALL = "利用率同比分析明细报表（整体）";
    String USAGE_RATE_YOY_ENERGY = "利用率同比分析明细报表（按能源）";
    String USAGE_RATE_YOY_LABEL = "利用率同比分析明细报表（按标签）";

    // 环比-折标煤

    String STANDARD_COAL_MOM_ALL = "折标煤用量环比分析明细报表（整体）";
    String STANDARD_COAL_MOM_ENERGY = "折标煤用量环比分析明细报表（按能源）";
    String STANDARD_COAL_MOM_LABEL = "折标煤用量环比分析明细报表（按标签）";

    // 环比-用能成本

    String COST_MOM_ALL = "用能成本环比分析明细报表（整体）";
    String COST_MOM_ENERGY = "用能成本环比分析明细报表（按能源）";
    String COST_MOM_LABEL = "用能成本环比分析明细报表（按标签）";

    // 环比-利用率

    String USAGE_RATE_MOM_ALL = "利用率环比分析明细报表（整体）";
    String USAGE_RATE_MOM_ENERGY = "利用率环比分析明细报表（按能源）";
    String USAGE_RATE_MOM_LABEL = "利用率环比分析明细报表（按标签）";


    // 定基比-折标煤

    String STANDARD_COAL_BENCHMARK_ALL = "折标煤用量定基比分析明细报表（整体）";
    String STANDARD_COAL_BENCHMARK_ENERGY = "折标煤用量定基比分析明细报表（按能源）";
    String STANDARD_COAL_BENCHMARK_LABEL = "折标煤用量定基比分析明细报表（按标签）";

    // 定基比-用能成本

    String COST_BENCHMARK_ALL = "用能成本定基比分析明细报表（整体）";
    String COST_BENCHMARK_ENERGY = "用能成本定基比分析明细报表（按能源）";
    String COST_BENCHMARK_LABEL = "用能成本定基比分析明细报表（按标签）";

    // 定基比-利用率

    String USAGE_RATE_BENCHMARK_ALL = "利用率定基比分析明细报表（整体）";
    String USAGE_RATE_BENCHMARK_ENERGY = "利用率定基比分析明细报表（按能源）";
    String USAGE_RATE_BENCHMARK_LABEL = "利用率定基比分析明细报表（按标签）";

    // 用能结构

    String STANDARD_COAL_STRUCTURE_ALL = "用能结构分析明细报表（整体）";
    String STANDARD_COAL_STRUCTURE_ENERGY = "用能结构分析明细报表（按能源）";
    String STANDARD_COAL_STRUCTURE_LABEL = "用能结构分析明细报表（按标签）";

    // 价格结构

    String COST_STRUCTURE_ALL = "价格结构分析明细报表（整体）";
    String COST_STRUCTURE_ENERGY = "价格结构分析明细报表（按能源）";
    String COST_STRUCTURE_LABEL = "价格结构分析明细报表（按标签）";

    // 个人化报表

    String SUPPLY_ANALYSIS  = "供应分析表";
    String STATISTICS_FEE  = "电费统计表";

    String WATER_STATISTICS = "水科报表";

    // 用电量统计

    String CONSUMPTION_STATISTICS_ALL = "用电量统计明细报表（整体）";
    String CONSUMPTION_STATISTICS_ENERGY = "用电量统计明细报表（按能源）";
    String CONSUMPTION_STATISTICS_LABEL = "用电量统计明细报表（按标签）";



    /**
     * 缺省表名
     */
    String DEFAULT = "default";
    /**
     * excel文件后缀
     */
    String XLSX = ".xlsx";
}
