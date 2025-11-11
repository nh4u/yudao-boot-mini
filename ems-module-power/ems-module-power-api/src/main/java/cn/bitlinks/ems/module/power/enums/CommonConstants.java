/*
 * Copyright (c) 2020 pig4cloud Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.bitlinks.ems.module.power.enums;

import java.util.regex.Pattern;

/**
 * @author liumingqiang
 */
public interface CommonConstants {

    /**
     * 标签树根节点
     */
    Long LABEL_TREE_ROOT_ID = 0L;
    /**
     * 10个->25个
     */
    Long LABEL_NUM_LIMIT = 25L;

    Long LABEL_LAYER_LIMIT = 5L;

    /**
     * 用能统计 标签最大展示深度（层级）
     */
    Integer LABEL_MAX_DISPLAY_DEEP = 5;

    /**
     * 每日合计
     */
    String DAILY_STATISTICS = "每日合计";

    /**
     * 每日合计
     */
    String MONTHLY_STATISTICS = "每月合计";
    /**
     * 每日合计
     */
    String ANNUAL_STATISTICS = "每年合计";

    String GROUP_ELECTRICITY = "电力";
    String GROUP_WATER = "水";

    Integer YEAR = 366;

    Integer DEFAULT_SCALE = 2;

    /**
     * 计量器具id
     */
    Long MEASUREMENT_INSTRUMENT_ID = 2L;
    /**
     * 重点设备id
     */
    Long KEY_EQUIPMENT_ID = 1L;
    /**
     * 其他设备标志
     */
    Long OTHER_EQUIPMENT_ID = 3L;
    /**
     * 折标煤
     */
    Integer STANDARD_COAL = 1;
    /**
     * 折价
     */
    Integer MONEY = 2;
    /**
     * 利用率
     */
    Integer RATIO = 3;
    /**
     * 点位1
     */
    Integer POINT_ONE = 1;
    /**
     * 点位2
     */
    Integer POINT_TWO = 2;
    String COAT_UNIT1 = "(kgce)";
    String COAT_UNIT2 = "(tce)";

    String COST_UNIT1 = "(元)";
    String COST_UNIT2 = "(万元)";

    String PCW = "PCW";
    String LTW = "低温水";
    String MTW = "中温水";
    String HRW = "热回收水/温水";
    String BHW = "锅炉热水";
    String MHW = "市政热水";
    /**
     * 低温水供水温度
     */
    String LTWT = "LTWT";
    /**
     * 中温水供水温度
     */
    String MTWT = "MTWT";
    /**
     * 热回收水供水温度
     */
    String HRWT = "HRWT";
    /**
     * 热水供水温度（锅炉出水）
     */
    String BHWT = "BHWT";
    /**
     * 热水供水温度（市政出水）
     */
    String MHWT = "MHWT";
    /**
     * PCW供水压力温度（供水压力）
     */
    String PCWP = "PCWP";
    /**
     * PCW供水压力温度（供水温度）
     */
    String PCWT = "PCWT";

    String DAY = "日";

    /**
     * 纯水
     */
    String PURE = "PURE";
    /**
     * 废水
     */
    String WASTE = "WASTE";
    /**
     * 压缩空气
     */
    String GAS = "GAS";
    /**
     * 30%NAOH（氢氧化钠）
     */
    String NAOH = "NAOH";
    /**
     * 30%HCL（盐酸）
     */
    String HCL = "HCL";
    /**
     * 自来水 W_Tap Water
     */
    String TW = "TW";
    /**
     * 高品质再生水 W_Reclaimed Water
     */
    String RW = "RW";
    /**
     * 电力
     */
    String DL = "DL";
    /**
     * 纯水供水量
     */
    String PW = "PW";
    /**
     * 废水量 ACW_FL505
     */
    String FL = "FL";
    /**
     * 废水量计量器具编号
     */
    String WASTE_WATER_STANDING_BOOK_CODE = "ACW_FL505";


    /**
     * 风向值
     */
    String WIND_DIRECTION_VALUE_IO = "PLC220.PLC.QxzData.FX";
    /**
     * 风向 东北IO地址
     */
    String WIND_DIRECTION_NE_IO = "PLC220.PLC.QxzDataFX_NE";
    /**
     * 风向 西北IO地址
     */
    String WIND_DIRECTION_NW_IO = "PLC220.PLC.QxzDataFX_NW";
    /**
     * 风向 东南IO地址
     */
    String WIND_DIRECTION_SE_IO = "PLC220.PLC.QxzDataFX_SE";
    /**
     * 风向 西南IO地址
     */
    String WIND_DIRECTION_SW_IO = "PLC220.PLC.QxzDataFX_SW";
    /**
     * 风速
     */
    String WIND_SPEED_IO = "PLC220.PLC.QxzData.FS";
    /**
     * 温度
     */
    String TEMPERATURE_IO = "PLC220.PLC.QxzData.WD";
    /**
     * 湿度
     */
    String HUMIDITY_IO = "PLC220.PLC.QxzData.SD";
    /**
     * 露点
     */
    String DEW_POINT_IO = "PLC220.PLC.DEWPOINT_OUTSIDE.PV";
    /**
     * 气压
     */
    String ATMOSPHERIC_PRESSURE_IO = "PLC220.PLC.QxzData.QY";
    /**
     * 噪音
     */
    String NOISE_IO = "PLC220.PLC.QxzData.ZY";

    String SYSTEM = "系统";

    String VOUCHER = "凭证";

    // HTTP请求方法

    String POST = "POST";
    String GET = "GET";
    String UTILIZATION_RATE_STR = "利用率";
    String CONVERSION_RATE_STR = "转换率";

    String ANALYSIS = "分析";

    /**
     * 随机成功概率
     */
    double SUCCESS_PROBABILITY = 0.8;
    /**
     * 服务名称（IP地址：端口号）协议
     */
    String SERVICE_NAME_FORMAT = "%s（%s：%s）%s";
    /**
     * 模板字符串中获取{}中参数
     */
    Pattern PATTERN_PARAMS = Pattern.compile("\\{(.*?)}");


    /**
     * 服务名称（IP地址：端口号）协议
     */
    String LABEL_NAME_PREFIX = "label_";


    /**
     * 告警策略任务，锁
     */
    String STRATEGY_TASK_LOCK_KEY = "strategy-task:%s";
    /**
     * cop聚合任务，锁
     */
    String COP_HOUR_AGG_TASK_LOCK_KEY = "cop-hour-agg-task:%s";
    /**
     * cop重算任务，锁
     */
    String COP_HOUR_AGG_RECALC_TASK_LOCK_KEY = "cop-hour-agg-recalc-task:%s";
    /**
     * 化学品录入任务，锁
     */
    String CHEMICALS_ADD_TASK_LOCK_KEY = "chemicals-add-task:%s";
    /**
     * 产量外部接口任务，锁
     */
    String PRODUCTION_SYNC_TASK_LOCK_KEY = "production-sync-task:%s";
    /**
     * 共享文件同步任务，锁
     */
    String SHARE_FILE_TASK_LOCK_KEY = "share-file-task:%s";

    String COP_HOUR_AGG_TABLE_NAME = "cop_hour_aggregate_data";
    /**
     * 拆分任务队列 redis key
     */
    String SPLIT_TASK_QUEUE_REDIS_KEY = "split_task_queue";
    /**
     * COP 重算队列
     */
    String COP_RECALCULATE_HOUR_QUEUE = "cop:recalc:hour:queue";


}
