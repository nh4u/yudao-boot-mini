use acquisition;
ALTER TABLE collect_raw_data MODIFY COLUMN `full_increment` INT ( 4 ) DEFAULT NULL COMMENT '全量/增量（0：全量；1增量。）';