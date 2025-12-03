update power_gas_measurement set sort_no = sort_no*10;
INSERT INTO `power_gas_measurement` (`id`, `measurement_name`, `measurement_code`, `sort_no`, `deleted`, `tenant_id`, `energy_param`)
VALUES (44, '普氮用量', 'GN2', 41, b'0', 1, '');
INSERT INTO `power_gas_measurement` (`id`, `measurement_name`, `measurement_code`, `sort_no`, `deleted`, `tenant_id`, `energy_param`)
VALUES (45, '高纯氮用量', 'PN2-CHQ', 161, b'0', 1, '');
INSERT INTO `power_gas_measurement` (`id`, `measurement_name`, `measurement_code`, `sort_no`, `deleted`, `tenant_id`, `energy_param`)
VALUES (46, '氢气用量', 'PH2-CHQ',  211, b'0', 1, '');
INSERT INTO `power_gas_measurement` (`id`, `measurement_name`, `measurement_code`, `sort_no`, `deleted`, `tenant_id`, `energy_param`)
VALUES (47, '氧气用量', 'PO2-CHQ', 291, b'0', 1, '');
INSERT INTO `power_gas_measurement` (`id`, `measurement_name`, `measurement_code`, `sort_no`, `deleted`, `tenant_id`, `energy_param`)
VALUES (48, '氩气用量', 'PAR-CHQ', 371, b'0', 1, '');
INSERT INTO `power_gas_measurement` (`id`, `measurement_name`, `measurement_code`, `sort_no`, `deleted`, `tenant_id`, `energy_param`)
VALUES (49, '氦气用量', 'PHE-CHQ', 431, b'0', 1, '');

INSERT INTO `system_dict_type` (`name`, `type`, `status`, `remark`) VALUES ('气化部分计算公式', 'gas_report_formula', 0, '气化部分计算公式');

INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`) VALUES ( 1, 'GN2', '(N2_GD_01_LL)-(N2_CHQ_01_LL)-(N2_CHQ_02_LL)', 'gas_report_formula', 0, '', '', '普氮用量：氮气主管道流量-高纯氮用量');
INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`) VALUES ( 2, 'PN2-CHQ', '(N2_CHQ_01_LL)+(N2_CHQ_02_LL)', 'gas_report_formula', 0, '', '', '高纯氮用量：1#氮气纯化器流量+2#氮气纯化器流量');
INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`) VALUES ( 3, 'PH2-CHQ', '(H2_CHQ_01_LL)+(H2_CHQ_02_LL)', 'gas_report_formula', 0, '', '', '氢气用量：1#氢气纯化器流量+2#氢气纯化器流量');
INSERT INTO `system_dict_data` ( `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`) VALUES (4, 'PO2-CHQ', '(O2_CHQ_01_LL)+(O2_CHQ_02_LL)', 'gas_report_formula', 0, '', '', '氧气用量：1#氧气纯化器流量+2#氧气纯化器流量');
INSERT INTO `system_dict_data` ( `sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`) VALUES ( 5, 'PAR-CHQ', '(AR_CHQ_01_LL)+(AR_CHQ_02_LL)', 'gas_report_formula', 0, '', '', '氩气用量：1#氩气纯化器流量+2#氩气纯化器流量');
INSERT INTO `system_dict_data` (`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`) VALUES (6, 'PHE-CHQ', '(HE_CHQ_01_LL)+(HE_CHQ_02_LL)', 'gas_report_formula', 0, '', '', '氦气用量：1#氦气纯化器流量+2#氦气纯化器流量');



