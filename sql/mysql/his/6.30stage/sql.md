

## SQL: starrocks 数据库

**1.执行create.sql**

2.执行crdalter1.sql、alter1.sql;

3.执行crdalter2.sql、alter2.sql;

4.执行crdalter3.sql、alter3.sql;

5.执行alter4.sql;

4. **更新所有表的分区到"2025-01-01 00:00:00"**,主要开始时间和结束时间，示例如下，注意结束时间

```sql
SHOW PARTITIONS FROM minute_aggregate_data;
ALTER TABLE minute_aggregate_data SET ("dynamic_partition.enable" = "false");
ALTER TABLE minute_aggregate_data
       ADD PARTITIONS START ("2025-01-01") END ("2025-06-27") EVERY (INTERVAL 1 DAY);
ALTER TABLE minute_aggregate_data SET ("dynamic_partition.enable" = "true");


SHOW PARTITIONS FROM usage_cost;
ALTER TABLE usage_cost SET ("dynamic_partition.enable" = "false");
ALTER TABLE usage_cost
       ADD PARTITIONS START ("2025-01-01") END ("2025-05-29") EVERY (INTERVAL 1 DAY);
ALTER TABLE usage_cost SET ("dynamic_partition.enable" = "true");


SHOW PARTITIONS FROM cop_hour_aggregate_data;
ALTER TABLE cop_hour_aggregate_data SET ("dynamic_partition.enable" = "false");
ALTER TABLE cop_hour_aggregate_data
       ADD PARTITIONS START ("2025-01-01") END ("2025-06-27") EVERY (INTERVAL 1 DAY);
ALTER TABLE cop_hour_aggregate_data SET ("dynamic_partition.enable" = "true");

```



## SQL：ydme_ems 数据库

1.执行ydme.sql、power_standingbook.sql、power_standingbook_attribute.sql、ems_header_code_mapping.sql