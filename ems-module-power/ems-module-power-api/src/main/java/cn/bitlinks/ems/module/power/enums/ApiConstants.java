package cn.bitlinks.ems.module.power.enums;

import cn.bitlinks.ems.framework.common.enums.RpcConstants;

import java.util.regex.Pattern;

/**
 * API 相关的枚举
 *
 * @author bitlinks
 */
public class ApiConstants {

    /**
     * 服务名
     *
     * 注意，需要保证和 spring.application.name 保持一致
     */
    public static final String NAME = "power-server";

    public static final String PREFIX = RpcConstants.RPC_API_PREFIX + "/power";

    public static final String VERSION = "1.0.0";
    /**
     * 设备属性-数据格式
     */
    // 文本
    public static final String TEXT = "TEXT";
    //数字
    public static final String NUMBER = "NUMBER";
    //日期yyyy-dd-mm
    public static final String DATE = "DATE";
    //单选下拉框
    public static final String SELECT = "SELECT";
    //多选下拉框
    public static final String MULTIPLE = "MULTIPLE";
    //日期时间yyyy-dd-mm hh:mm:ss
    public static final String DATETIME = "DATETIME";
    //文件
    public static final String FILE = "FILE";
    //图片
    public static final String PICTURE = "PICTURE";
    /**
     * 设备属性-是否必填
     */
    public static final String YES = "0";
    public static final String NO = "1";

    public static final String SYSTEM_CREATE ="系统创建";
    public static final String PARENT_ATTR_AUTO ="父节点属性自动生成";
    // 台账
    public static final String SB_TYPE_ATTR_TOP_TYPE="topType";
    public static final String ATTR_STAGE ="stage";
    public static final String ATTR_TYPE_ID ="typeId";
    public static final String ATTR_SB_TYPE_ID ="standingbookTypeId";
    public static final String ATTR_CREATE_TIME ="createTime";
    public static final String ATTR_LABEL_INFO ="labelInfo";
    public static final String ATTR_LABEL_INFO_PREFIX ="label_";
    public static final String ATTR_TABLE_TYPE ="tableType";
    public static final String ATTR_VALUE_TYPE ="valueType";
    public static final String ATTR_MEASURING_INSTRUMENT_MAME ="measuringInstrumentName";
    public static final String ATTR_MEASURING_INSTRUMENT_ID ="measuringInstrumentId";
    public static final String ATTR_EQUIPMENT_ID ="equipmentId";
    public static final String ATTR_EQUIPMENT_NAME ="equipmentName";
    public static final String ATTR_ENERGY ="energy";
    public static final String SQL_SB_ID ="sbId";

    // 告警管理-查看详情（跳转设备）todo 设备监控设备跳转链接待完善
    public static final String SB_MONITOR_DETAIL ="<a href=\"/aa/aa?id=%s\">查看详情</a>";
    /**
     * 数采公式：匹配格式获取公式中的参数
     */
    public static final Pattern PATTERN_ACQUISITION_FORMULA_PARAM = Pattern.compile("\\{\\[\"([^\"]+)\"," +
            " (true|false|\"[^\"]+\")\\]\\}");
    /**
     * 数采公式：填充参数格式 code、energyFlag
     */
    public static final String  PATTERN_ACQUISITION_FORMULA_FILL =
            "{[\"%s\", %s]}";



}
