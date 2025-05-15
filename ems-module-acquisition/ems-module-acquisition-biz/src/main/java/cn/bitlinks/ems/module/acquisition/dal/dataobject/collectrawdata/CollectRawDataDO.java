//package cn.bitlinks.ems.module.acquisition.dal.dataobject.collectrawdata;
//
///**
// * 实时数据
// */
//public class CollectRawDataDO {
//}
//    CREATE TABLE `collect_raw_data` (
//        `data_site` varchar(255) NOT NULL COMMENT "OPC_DA:IO地址/",
//        `sync_time` datetime NOT NULL COMMENT "数据同步时间",
//        `param_code` varchar(255) NOT NULL COMMENT "参数 code",
//        `energy_flag` tinyint(4) NOT NULL COMMENT "是否能源数采参数 0自定义 1能源参数",
//        `param_type` int(11) NOT NULL COMMENT "参数类型",
//        `standingbook_id` bigint(20) NOT NULL COMMENT "台账id",
//        `calc_value` varchar(255) NULL COMMENT "公式计算值",
//        `raw_value` varchar(255) NULL COMMENT "采集值（原始）",
//        `collect_time` datetime NOT NULL COMMENT "数据采集时间（原始）",
//        `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT "创建时间"