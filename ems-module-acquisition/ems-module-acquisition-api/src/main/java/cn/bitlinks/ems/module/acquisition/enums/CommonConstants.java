package cn.bitlinks.ems.module.acquisition.enums;

public interface CommonConstants {
    /**
     * 数据采集任务前缀
     */
    String ACQUISITION_JOB_NAME_PREFIX = "ACQUISITION_JOB_%s";
    /**
     * 数据采集任务,数据map 键-数采设置参数详情
     */
    String ACQUISITION_JOB_DATA_MAP_KEY_DETAILS = "acquisitionJobDetails";
    /**
     * 数据采集任务,数据map 键-台账id
     */
    String ACQUISITION_JOB_DATA_MAP_KEY_STANDING_BOOK_ID = "acquisitionJobStandingBookId";
    /**
     * 数据采集任务,数据map 键-服务设置
     */
    String ACQUISITION_JOB_DATA_MAP_KEY_SERVICE_SETTINGS = "acquisitionJobServiceSettings";
    /**
     * 数据采集任务,数据map 键-设备任务状态
     */
    String ACQUISITION_JOB_DATA_MAP_KEY_STATUS = "acquisitionJobStatus";

    /**
     * 数据采集任务redis 前缀 env:io地址
     */
    String ACQUISITION_JOB_REDIS_KEY = "ACQUISITION_JOB_LATEST_VALUE_%s:%s";
    String STREAM_LOAD_PREFIX = "_streamload_";
    String STREAM_LOAD_BUFFER_PREFIX = "_buffer_streamload_";
    /**
     * 聚合任务锁
     */
    String AGG_TASK_LOCK_KEY = "agg-task:%s";
    String AGG_TASK_STEADY_LOCK_KEY = "agg-steady-task:%s";
    String MINUTE_AGGREGATE_DATA_TB_NAME = "minute_aggregate_data";
    String USAGE_COST_TB_NAME = "usage_cost";
    String COP_HOUR_AGGREGATE_DATA_TB_NAME = "cop_hour_aggregate_data";
    int batchSize = 2000;
    /**
     * redis 创建分区的上限分区:dev:tbName
     */
    String REDIS_KEY_MAX_PARTITION_TIME = "max-partition-time:%s:%s";

    /**
     * redis 手动维护的历史分区列表
     */
    String REDIS_KEY_HIS_PARTITION_LIST = "his-partition-list:%s:%s";
    /**
     * 分区关闭动态
     */
    String DISABLE_DYNAMIC_PARTITION_SQL = "ALTER TABLE %s SET (\"dynamic_partition.enable\" = \"false\")";
    /**
     * 添加分区语句
     */
    String ADD_PARTITIONS_SQL = "ALTER TABLE %s ADD PARTITION %s VALUES [(\"%s\"), (\"%s\"))";
    /**
     * 分区开启动态
     */
    String ENABLE_DYNAMIC_PARTITION_SQL = "ALTER TABLE %s SET (\"dynamic_partition.enable\" = \"true\")";
    /**
     * 拆分任务队列 redis key
     */
    String SPLIT_TASK_QUEUE_REDIS_KEY_PREFIX = "split_task_queue:";
    /**
     * 拆分任务队列 redis key
     */
    String SPLIT_TASK_QUEUE_REDIS_KEY_PATTERN = "split_task_queue:*";
}


