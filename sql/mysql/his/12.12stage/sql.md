
####  修改配置项
```sql
INSERT INTO `ydme_ems`.`infra_config` (`category`, `type`, `name`, `config_key`, `value`, `visible`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES ('biz', 1, '全厂', 'power.label.all', '204,205#204,209', b'0', '全厂（用能单位->燕东科技、用能单位->高可靠）', 'admin', '2025-12-03 10:29:22', '', '2025-12-03 16:55:45', b'0');
INSERT INTO `ydme_ems`.`infra_config` (`category`, `type`, `name`, `config_key`, `value`, `visible`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`) VALUES ('biz', 1, '用电量统计默认标签', 'power.electric.default.labels', '[[248, 251, 258],[204,205,207]]', b'1', '用电量统计默认展示标签', '', '2025-12-03 11:42:04', '', '2025-12-03 16:40:12', b'0');

```
