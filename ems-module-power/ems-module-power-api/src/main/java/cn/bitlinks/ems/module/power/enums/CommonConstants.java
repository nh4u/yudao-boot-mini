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

    String ELECTRICITY = "电力";

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

    String COP_HOUR_AGG_TABLE_NAME = "cop_hour_aggregate_data";
    /**
     * 拆分任务队列 redis key
     */
    String SPLIT_TASK_QUEUE_REDIS_KEY_PREFIX = "split_task_queue:";

}
