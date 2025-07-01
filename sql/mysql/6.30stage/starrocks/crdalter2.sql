use acquisition;
ALTER TABLE collect_raw_data MODIFY COLUMN `data_feature` INT ( 1 ) DEFAULT NULL COMMENT '数据特征 1累计值2稳态值3状态值';