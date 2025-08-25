use acquisition;
ALTER TABLE collect_raw_data MODIFY COLUMN `data_type` INT ( 1 ) DEFAULT NULL COMMENT '数据类型 1数字2文本';