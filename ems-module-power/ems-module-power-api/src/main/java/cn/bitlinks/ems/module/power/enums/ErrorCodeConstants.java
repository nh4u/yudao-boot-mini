package cn.bitlinks.ems.module.power.enums;

import cn.bitlinks.ems.framework.common.exception.ErrorCode;

/**
 * Infra 错误码枚举类
 * <p>
 * infra 系统，使用 1-001-000-000 段
 */
public interface ErrorCodeConstants {
    // ========== 能源配置 1-001-301-000 ==========
    ErrorCode COAL_FACTOR_HISTORY_NOT_EXISTS = new ErrorCode(1_001_301_000, "折标煤系数历史不存在");
    ErrorCode ENERGY_CONFIGURATION_NOT_EXISTS = new ErrorCode(1_001_301_002, "能源配置不存在");
    ErrorCode UNIT_PRICE_CONFIGURATION_NOT_EXISTS = new ErrorCode(1_001_301_003, "单价配置不存在");
    ErrorCode TIME_CONFLICT = new ErrorCode(1_001_301_004, "与已有单价时间重叠");
    ErrorCode INVALID_PRICE_TYPE = new ErrorCode(1_001_301_010, "无效的价格类型");
    ErrorCode INVALID_TIME_TYPE = new ErrorCode(1_001_301_011, "无效的时间类型");
    ErrorCode ENERGY_CODE_DUPLICATE = new ErrorCode(1_001_301_006, "能源编码重复");
    ErrorCode ENERGY_NAME_DUPLICATE = new ErrorCode(1_001_301_007, "能源名称重复");
    ErrorCode ENERGY_CONFIGURATION_STANDINGBOOK_EXISTS = new ErrorCode(1_001_301_008, "该能源关联计量器具，不可进行删除");
    ErrorCode ENERGY_CONFIGURATION_STANDINGBOOK_UNIT = new ErrorCode(1_001_301_009, "该能源已有参数单位，且已关联计量器具，不可进行修改");
    ErrorCode FAILED_PRICE_DETAILS = new ErrorCode(1_001_301_012,"解析价格详情失败");
    ErrorCode END_TIME_MUST_AFTER_START_TIME = new ErrorCode(1_001_301_013,"结束时间必须大于开始时间");
    ErrorCode PAST_PERIOD_MODIFY_NOT_ALLOWED = new ErrorCode(1_001_301_014,"过去的应用周期不可修改");
    ErrorCode CANNOT_MODIFY_START_TIME_OF_CURRENT_PERIOD = new ErrorCode(1_001_301_015,"当前周期的开始时间不可修改");
    ErrorCode INVALID_END_TIME_FOR_CURRENT_PERIOD = new ErrorCode(1_001_301_016,"结束时间必须大于当前时间且大于开始时间");
    ErrorCode NEXT_PERIOD_CONFLICT = new ErrorCode(1_001_301_017,"存在后续周期配置，请先清空下一周期时间范围！");
    ErrorCode CANNOT_DELETE_PAST_PERIOD = new ErrorCode(1_001_301_018,"过去的应用周期不可删除");
    ErrorCode CANNOT_DELETE_CURRENT_PERIOD = new ErrorCode(1_001_301_019,"当前应用周期不可删除");
    ErrorCode PRICE_DETAIL_NOT_EXISTS = new ErrorCode(1_001_301_020,"单价详细不存在");
    ErrorCode INVALID_FIXED_PRICE_DETAILS = new ErrorCode(1_001_301_021,"统一计价模式下必须且只能设置一个全局单价");
    ErrorCode INVALID_LADDER_CONTINUITY = new ErrorCode(1_001_301_022,"阶梯计价连续性无效，请确保用量范围连续");
    ErrorCode FIXED_PRICE_DETAILS_NOT_FOUND = new ErrorCode(1_001_301_023,"单价详细不存在");
    ErrorCode NO_MATCHING_TIME_PERIOD = new ErrorCode(1_001_301_024,"该时间段不存在");
    ErrorCode ACCOUNTING_FREQUENCY_NOT_SET = new ErrorCode(1_001_301_025,"核算频率为空");
    ErrorCode ENERGY_PARAMETERS_NOT_EXISTS = new ErrorCode(1_001_301_026,"能源参数不存在");
    ErrorCode USAGE_MORE_THAN_ONE = new ErrorCode(1_001_301_027,"每个能源只能有一个用量参数");
    ErrorCode ENERGY_CONFIGURATION_STANDINGBOOK_DELETE = new ErrorCode(1_001_301_027,"该能源已关联计量器具，不可删除能源参数");
    ErrorCode ENERGY_ASSOCIATED_STANDINGBOOK_UPDATE = new ErrorCode(1_001_301_027,"该能源存在关联数据，不可修改能源来源");
    ErrorCode ENERGY_PARAMETER_CODE_DUPLICATE = new ErrorCode(1_001_301_028,"能源参数不可重复");
    ErrorCode ENERGY_CONFIGURATION_TEMPLATE_ASSOCIATED = new ErrorCode(1_001_301_029,"该能源参数已关联计量器具模板，不可删除或更新");
    ErrorCode ENERGY_ID_NOT_EXISTS = new ErrorCode(1_001_301_030, "能源id不存在");
    ErrorCode ENERGY_PARAMS_NAME_REPEAT = new ErrorCode(1_001_301_031, "同一能源参数名称不可重复");
    // ========== 能源分组 ==========
    ErrorCode ENERGY_GROUP_NOT_EXISTS = new ErrorCode(1_001_301_101, "能源分组不存在");
    ErrorCode ENERGY_GROUP_LIST_NOT_EXISTS = new ErrorCode(1_001_301_102, "能源分组list不存在");

    ErrorCode ENERGY_GROUP_EXISTS = new ErrorCode(1_001_301_103, "能源分组已存在");

    //========== 关联下级计量和关联上级设备 配置 1-001-101-000 ==========
    ErrorCode DEVICE_ASSOCIATION_CONFIGURATION_NOT_EXISTS = new ErrorCode(1_001_101_001, "设备关联配置不存在");

    //========== 数采补录 1-001-301-005 ==========
    ErrorCode ADDITIONAL_RECORDING_NOT_EXISTS = new ErrorCode(1_001_301_015, "补录不存在");
    ErrorCode THIS_TIME_EXISTS_DATA = new ErrorCode(1_001_301_201, "该时间点已存在数据，请选择其他时间");
    ErrorCode THIS_VALUE_NOT_LESS = new ErrorCode(1_001_301_202, "本次数值不可小于上次数值");
    ErrorCode THIS_VALUE_NOT_MORE = new ErrorCode(1_001_301_203, "本次数值不可大于下次数值");
    ErrorCode VALUE_TYPE_REQUIRED = new ErrorCode(1_001_301_204, "该计量器具能源用量的数值特征为累积值，需要指定数据是全量还是增量");
    ErrorCode PURCHASE_TIME_OVER_CURRENT = new ErrorCode(1_001_301_205, "购入时间不可大于当前时间");
    ErrorCode ADDITIONAL_RECORDING_ENERGY_NOT_EXISTS = new ErrorCode(1_001_301_206, "无对应能源用量，不可进行补录");
    ErrorCode CURRENT_TIME_ERROR=new ErrorCode(1_001_301_207,"补录时间不可大于当前时间");
    ErrorCode CURRENT_TIME_TOO_BIG_ERROR=new ErrorCode(1_001_301_208,"本次采集数据值不可大于已有数据！");
    ErrorCode CURRENT_TIME_TOO_SMALL_ERROR=new ErrorCode(1_001_301_209,"本次采集数据值不可小于已有数据！");
    ErrorCode INCREMENT_HISTORY_NOT_EXISTS=new ErrorCode(1_001_301_210,"无历史数据，不可单独补录增量，请选择全量进行补录！");
    ErrorCode PRE_COLLECT_TIME_ERROR=new ErrorCode(1_001_301_211,"上次采集时间与历史数据不连续，不可进行补录！");
    ErrorCode CUR_COLLECT_TIME_ERROR=new ErrorCode(1_001_301_212,"本次采集时间与历史数据不连续，不可进行补录！");
    ErrorCode RANGE_TIME_NOT_NULL=new ErrorCode(1_001_301_213,"上次采集时间和本次采集时间不能为空");

    ErrorCode RANGE_TIME_ERROR=new ErrorCode(1_001_301_214,"上次采集时间和本次采集时间时间范围与历史数据不连续，不可进行补录！");
    ErrorCode ADDITIONAL_RECORDING_SPLIT_ERROR = new ErrorCode(1_001_301_215, "补录拆分失败，无法补录");
    ErrorCode LAST_COLLECT_TIME_TOO_LATE = new ErrorCode(1_001_301_216, "上次采集时间需早于本次采集时间");
    ErrorCode MINUTE_DATA_HISTORY_NOT_EXIST=new ErrorCode(1_001_301_217,"无历史数据，无法补录");
    ErrorCode INCREMENT_TIME_NOT_NULL = new ErrorCode(1_001_301_218,"增量补录起止时间不能为空");
    ErrorCode INCREMENT_VALUE_MUST_GT_ZERO = new ErrorCode(1_001_301_219,"增量必须大于等于0");
    ErrorCode FULL_INCREMENT_TYPE_ERROR = new ErrorCode(1_001_301_220,"请选择全量/增量");
    ErrorCode FULL_TIME_NOT_NULL = new ErrorCode(1_001_301_221,"全量补录时间不能为空");
    ErrorCode FULL_VALUE_MUST_GT_LEFT = new ErrorCode(1_001_301_222,"全量值必须大于等于上一全量值{}");
    ErrorCode FULL_VALUE_MUST_LT_RIGHT = new ErrorCode(1_001_301_223,"全量值必须小于等于下一全量值{}");

    ErrorCode IMPORT_ACQ_MISTAKE = new ErrorCode(1_001_301_224,"采集点错误");
    ErrorCode IMPORT_ACQ_MISTAKE_DETAIL = new ErrorCode(1_001_301_225,"采集点不存在或未匹配，请检查采集点与系统采集点编号是否完全一致");
    ErrorCode IMPORT_EXCEL_ERROR = new ErrorCode(1_001_301_226,"Excel解析失败");
    ErrorCode IMPORT_DATA_STREAM_LOAD_ERROR = new ErrorCode(1_001_301_227,"数据插入失败");
    ErrorCode IMPORT_NO_TIMES = new ErrorCode(1_001_301_228,"时间选择有误，读取不到有效时间");
    ErrorCode IMPORT_TIMES_ERROR = new ErrorCode(1_001_301_229,"时间选择有误，存在非时间数据");
    ErrorCode IMPORT_NO_METER = new ErrorCode(1_001_301_230,"采集点选择有误，读取不到有效数据");

    // ========== 外部接口 ==========
    ErrorCode EXTERNAL_API_NOT_EXISTS = new ErrorCode(1_001_301_240,"外部接口不存在");

    ErrorCode EXTERNAL_API_CODE_REPEAT = new ErrorCode(1_001_301_241,"外部接口编码重复");

    ErrorCode EXTERNAL_API_HTTP_METHOD_ERROR = new ErrorCode(1_001_301_242,"请求方式不匹配");

    // ========== 台账类型 ==========
    ErrorCode STANDINGBOOK_TYPE_NOT_EXISTS = new ErrorCode(1_001_202_000, "台账类型不存在");
    ErrorCode STANDINGBOOK_TYPE_EXITS_CHILDREN = new ErrorCode(1_001_202_001, "存在存在子台账类型，无法删除");
    ErrorCode STANDINGBOOK_TYPE_PARENT_NOT_EXITS = new ErrorCode(1_001_202_002, "父级台账类型不存在");
    ErrorCode STANDINGBOOK_TYPE_PARENT_ERROR = new ErrorCode(1_001_202_003, "不能设置自己为父台账类型");
    //ErrorCode STANDINGBOOK_TYPE_NAME_DUPLICATE = new ErrorCode(1_001_202_004, "已经存在该名字的台账类型");
    ErrorCode STANDINGBOOK_TYPE_CODE_DUPLICATE = new ErrorCode(1_001_202_004, "台账类型编码已存在");

    ErrorCode STANDINGBOOK_TYPE_PARENT_IS_CHILD = new ErrorCode(1_001_202_005, "不能设置自己的子StandingboookType为父StandingboookType");
    ErrorCode STANDINGBOOK_ATTRIBUTE_NOT_EXISTS = new ErrorCode(1_001_202_006, "台账属性不存在");
    ErrorCode STANDINGBOOK_NOT_EXISTS = new ErrorCode(1_001_202_007, "台账不存在");
    ErrorCode STANDINGBOOK_EXISTS = new ErrorCode(1_001_202_008, "该分类存在设备，不可进行删除");

    ErrorCode STANDINGBOOK_CODE_REPEAT_CHILDREN = new ErrorCode(1_001_202_009, "模板编码与子级台账类型编码重复，请修改后再提交");
    ErrorCode STANDINGBOOK_CODE_EXISTS = new ErrorCode(1_001_202_010, "编号已存在，请重新输入！");
    ErrorCode STANDINGBOOK_NO_ATTR = new ErrorCode(1_001_202_011, "台账没有台账属性");
    ErrorCode STANDINGBOOK_TYPE_ONLY_FIVE = new ErrorCode(1_001_202_012, "最多允许五层节点");
    ErrorCode STANDINGBOOK_EXIST_NOT_SUPPORT_UPD_DEL = new ErrorCode(1_001_202_013, "该分类或其子分类下存在设备，不可删除和修改！");
    ErrorCode STANDINGBOOK_ASSOCIATION_EXISTS = new ErrorCode(1_001_202_014,"已与其他计量器具关联，请先将关联关系删除");
    ErrorCode STANDINGBOOK_ACQUISITION_EXISTS = new ErrorCode(1_001_202_015,"已进行数采配置，请先停用数采配置");
    ErrorCode STANDINGBOOK_TYPE_REL_STANDINGBOOK = new ErrorCode(1_001_202_016,"存在设备数据，不允许修改名称和编码");
    ErrorCode STANDINGBOOK_EXIST_NOT_SUPPORT_CREATE = new ErrorCode(1_001_202_017, "该分类或其子分类下存在设备，不可选择能源！");

    ErrorCode STANDINGBOOK_REL_STRATEGY = new ErrorCode(1_001_202_018,"已配置告警策略，请先删除告警策略");


    // ========== 标签配置 ==========
    ErrorCode LABEL_CONFIG_NOT_EXISTS = new ErrorCode(1_001_401_001, "配置标签不存在");
    ErrorCode LABEL_CONFIG_REACH_LIMIT = new ErrorCode(1_001_401_002, "单层标签超过限制");
    ErrorCode LABEL_CONFIG_REACH_LAYER_LIMIT = new ErrorCode(1_001_401_003, "标签层数超过限制");
    ErrorCode LABEL_CONFIG_CODE_DUPLICATE = new ErrorCode(1_001_401_004, "标签编码已存在");
    ErrorCode LABEL_CONFIG_CODE_REQUIRED = new ErrorCode(1_001_401_005, "标签编码不能为空");
    ErrorCode LABEL_CONFIG_HAS_DEVICE = new ErrorCode(1006, "该标签存在设备，不可进行删除");
    ErrorCode LABEL_CONFIG_CHILDREN_HAS_DEVICE = new ErrorCode(1006, "该标签子级存在设备，不可进行删除");

    // ========== 凭证管理 ==========
    ErrorCode VOUCHER_NOT_EXISTS = new ErrorCode(1_001_501_001, "凭证不存在");
    ErrorCode VOUCHER_LIST_IS_EMPTY = new ErrorCode(1_001_501_002, "凭证ID列表为空");
    ErrorCode VOUCHER_USAGE_MODIFIED_ERROR = new ErrorCode(1_001_501_003, "凭证ID已在数据补录中使用，无法修改用量值");
    ErrorCode VOUCHER_HAS_ADDITIONAL_RECORDING = new ErrorCode(1_001_501_004, "该凭证信息已关联补录数据，不可进行删除！");



    // ========== 告警管理 ==========
    ErrorCode WARNING_INFO_NOT_EXISTS = new ErrorCode(1_001_701_001, "告警信息不存在");
    ErrorCode WARNING_TEMPLATE_NOT_EXISTS = new ErrorCode(1_001_701_002, "告警模板不存在");
    ErrorCode WARNING_TEMPLATE_CODE_EXISTS = new ErrorCode(1_001_701_003, "模版编码({}) 已存在");
    ErrorCode WARNING_TEMPLATE_DELETE_ERROR = new ErrorCode(1_001_701_004, "该模板已关联告警规则，不可删除！");
    ErrorCode WARNING_TEMPLATE_DELETE_BATCH_ERROR = new ErrorCode(1_001_701_005, "{}模板已关联告警规则，不可删除！");
    ErrorCode WARNING_STRATEGY_NOT_EXISTS = new ErrorCode(1_001_701_006, "告警策略不存在");
    ErrorCode WARNING_STRATEGY_CONDITION_NOT_NULL = new ErrorCode(1_001_701_007, "告警策略条件不能为空");
    ErrorCode WARNING_INFO_NO_CONTENT = new ErrorCode(1_001_701_008, "该告警信息无内容");
    ErrorCode WARNING_TEMPLATE_CONTENT_ILLEGAL = new ErrorCode(1_001_701_009, "模板内容关键字不正确");
    ErrorCode WARNING_TEMPLATE_TITLE_ILLEGAL = new ErrorCode(1_001_701_010, "模板主题关键字不正确");

    // ========== 公式模块 ==========
    ErrorCode FORMULA_TYPE_NOT_EXISTS = new ErrorCode(1_001_801_001, "公式类型不存在");
    ErrorCode FORMULA_NOT_EXISTS = new ErrorCode(1_001_801_002, "参数公式不存在");
    ErrorCode FORMULA_LIST_NOT_EXISTS = new ErrorCode(1_001_801_003, "公式list不存在");
    ErrorCode FORMULA_HAVE_EXISTS = new ErrorCode(1_001_801_004, "已存在相同的公式，不可重复添加");
    ErrorCode FORMULA_ID_NOT_EXISTS = new ErrorCode(1_001_801_005, "公式id不存在");
    ErrorCode FORMULA_HAVE_BIND_DELETE = new ErrorCode(1_001_801_006, "公式已使用，不可删除！");
    ErrorCode FORMULA_HAVE_BIND_UPDATE = new ErrorCode(1_001_801_006, "公式已使用，不可更新！");
    ErrorCode EXPRESSION_EXECUTION_FAILED = new ErrorCode(1_001_801_007, "执行表达式失败！");
    // ========== 其他业务错误 ==========

    ErrorCode DATE_RANGE_NOT_EXISTS = new ErrorCode(1_001_601_001, "日期范围不存在");
    ErrorCode DATE_RANGE_EXCEED_LIMIT = new ErrorCode(1_001_601_002, "日期范围超出限制（MAX：366）");
    ErrorCode QUERY_TYPE_NOT_EXISTS = new ErrorCode(1_001_601_003, "查看类型不存在");
    ErrorCode DATE_TYPE_NOT_EXISTS = new ErrorCode(1_001_601_004, "时间类型不存在");
    ErrorCode BENCH_MARK_NOT_EXISTS = new ErrorCode(1_001_601_005, "基准年限不存在");
    ErrorCode SYSTEM_TYPE_NOT_EXISTS = new ErrorCode(1_001_601_006, "系统类型不存在");
    ErrorCode DATE_TYPE_NOT_MATCH = new ErrorCode(1_001_601_006, "时间类型不匹配");
    // ========== 服务设置  ==========
    ErrorCode SERVICE_SETTINGS_NOT_EXISTS = new ErrorCode(1_001_901_001, "服务设置不存在");
    ErrorCode SERVICE_SETTINGS_ADD_ERROR = new ErrorCode(1_001_901_002, "服务设置添加失败，请检查服务设置信息是否正确");
    ErrorCode SERVICE_SETTINGS_IP_REPEAT = new ErrorCode(1_001_901_003, "服务设置IP已存在");
    ErrorCode SERVICE_SETTINGS_REFUSE_DELETE = new ErrorCode(1_001_901_004, "服务设置已关联设备数采，不可删除");
    ErrorCode SERVICE_SETTINGS_REFUSE_UPD = new ErrorCode(1_001_901_005, "服务设置已关联设备数采，只可修改服务名称和重试次数");
    // ========== 数采设置  ==========
    ErrorCode STANDINGBOOK_ACQUISITION_NOT_EXISTS = new ErrorCode(1_002_101_001, "台账-数采设置不存在");
    ErrorCode STANDINGBOOK_ACQUISITION_FORMULA_SET = new ErrorCode(1_002_101_002,"公式配置有误，无法进行数据采集，请检查");
    ErrorCode STANDINGBOOK_ACQUISITION_CYCLE_RELY =  new ErrorCode(1_002_101_003,"公式配置存在循环依赖");
    ErrorCode STANDINGBOOK_ACQUISITION_TEST_FAIL =  new ErrorCode(1_002_101_004,"采集失败");
    ErrorCode STANDINGBOOK_ACQUISITION_CRON_FAIL =  new ErrorCode(1_002_101_005,"频率和单位不能为空且频率必须大于0");
    ErrorCode STANDINGBOOK_ACQUISITION_CRON_UNIT_NOT_EXISTS =  new ErrorCode(1_002_101_006,"无效的频率单位: {}");


    // ========== COP参数设置  ==========
    ErrorCode COP_SETTINGS_NOT_EXISTS = new ErrorCode(1_001_102_001, "COP参数设置不存在");
    ErrorCode STANDINGBOOK_REPEAT = new ErrorCode(1_001_102_002, "COP参数关联台账重复");
    ErrorCode COP_SETTINGS_LIST_NOT_EXISTS = new ErrorCode(1_001_102_003, "COP参数list不存在");
    ErrorCode COP_SETTINGS_STANDINGBOOK_NOT_EMPTY = new ErrorCode(1_001_102_003, "COP公式参数关联的台账不能为空");

    // ========== 供应分析参数设置  ==========

    ErrorCode SUPPLY_ANALYSIS_STANDINGBOOK_REPEAT = new ErrorCode(1_001_102_010, "供应分析关联台账重复");
    ErrorCode SUPPLY_ANALYSIS_SETTINGS_LIST_NOT_EXISTS = new ErrorCode(1_001_102_011, "供应分析list不存在");
    ErrorCode SUPPLY_ANALYSIS_SETTINGS_STANDINGBOOK_NOT_EMPTY = new ErrorCode(1_001_102_012, "供应分析关联的台账不能为空");

    ErrorCode TEAM_NOT_EMPTY = new ErrorCode(1_001_102_013, "班组不能为空");
    ErrorCode TEAM_NOT_MATCH = new ErrorCode(1_001_102_014, "班组不匹配 只能0或1");
    ErrorCode TEAM_NOT_INPUT = new ErrorCode(1_001_102_015, "按时统计时，不应传班组");
    // ========== 用能分析  ==========
    ErrorCode UNIT_NOT_EMPTY = new ErrorCode(1_001_103_001, "单位不能为空");
    //======== 个性报表
    ErrorCode DUPLICATE_TRANSFORMER_ID = new ErrorCode(1_001_301_001, "变压器不可重复");
    // ======双碳接口
    ErrorCode DOUBLE_CARBON_CODE_DUPLICATE = new ErrorCode(1_001_302_001, "双碳编码已存在");

}
