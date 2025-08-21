use acquisition;
ALTER TABLE `minute_aggregate_data` MODIFY COLUMN `aggregate_time` datetime NOT NULL COMMENT "聚合时间";
ALTER TABLE `minute_aggregate_data` MODIFY COLUMN `param_code` varchar(255) NOT NULL COMMENT "参数 code";
ALTER TABLE `minute_aggregate_data` MODIFY COLUMN `energy_flag` tinyint(4) NOT NULL COMMENT "是否能源数采参数 0自定义 1能源参数";
ALTER TABLE `minute_aggregate_data` MODIFY COLUMN `standingbook_id` bigint(20) NOT NULL COMMENT "台账id";
ALTER TABLE `minute_aggregate_data` MODIFY COLUMN `data_site` varchar(255) NULL COMMENT "OPC_DA:IO地址/";
ALTER TABLE `minute_aggregate_data` MODIFY COLUMN `full_value` decimal(30, 10) NULL COMMENT "全量（累积值）";
ALTER TABLE `minute_aggregate_data` MODIFY COLUMN `incremental_value` decimal(30, 10) NULL COMMENT "增量（累积值）";
ALTER TABLE `minute_aggregate_data` MODIFY COLUMN `full_increment` int(11) NULL COMMENT "全量/增量（0：全量；1增量。）";
ALTER TABLE `minute_aggregate_data` MODIFY COLUMN `data_feature` int(11) NULL COMMENT "数据特征 1累计值2稳态值3状态值";
ALTER TABLE `minute_aggregate_data` MODIFY COLUMN `data_type` int(11) NULL COMMENT "数据类型 1数字2文本";
ALTER TABLE `minute_aggregate_data` MODIFY COLUMN `usage` int(11) NULL COMMENT "用量1，非用量0";
ALTER TABLE `minute_aggregate_data` MODIFY COLUMN `acq_flag` int(11) NULL COMMENT "业务点1，不是业务点0";



ALTER TABLE `cop_hour_aggregate_data` MODIFY COLUMN `aggregate_time` datetime NOT NULL COMMENT "聚合时间";
ALTER TABLE `cop_hour_aggregate_data` MODIFY COLUMN `cop_type` varchar(40) NOT NULL COMMENT "低温冷机 LTC,低温系统 LTS,中温冷机 MTC,中温系统 MTS";
ALTER TABLE `cop_hour_aggregate_data` MODIFY COLUMN `cop_value` decimal(30, 10) NULL COMMENT "公式计算值";
