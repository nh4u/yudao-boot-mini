## 策略+字段
ALTER TABLE power_warning_strategy
ADD COLUMN `last_exp_time` datetime DEFAULT NULL COMMENT '最新触发时间';

## 策略条件表-修改standingbook_id字段类型
ALTER TABLE power_warning_strategy_condition
MODIFY COLUMN strategy_id BIGINT(20) NOT NULL COMMENT '策略id';
## 删除台账excel表头映射表