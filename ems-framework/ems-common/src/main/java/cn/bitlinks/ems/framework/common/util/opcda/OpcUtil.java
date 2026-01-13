package cn.bitlinks.ems.framework.common.util.opcda;

import cn.hutool.core.date.DateUtil;
import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.core.JIArray;
import org.jinterop.dcom.core.JIVariant;
import org.openscada.opc.lib.da.AddFailedException;
import org.openscada.opc.lib.da.Group;
import org.openscada.opc.lib.da.Item;
import org.openscada.opc.lib.da.ItemState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class OpcUtil {
    private static final Logger logger = LoggerFactory.getLogger(OpcUtil.class);
    // 缓存点位与Item的映射，避免重复创建
    private static final Map<String, Item> ITEM_CACHE = new ConcurrentHashMap<>();
    // 记录失败点位及次数，用于熔断
    private static final Map<String, Integer> FAIL_COUNT_MAP = new ConcurrentHashMap<>();
    // 连续失败阈值，超过则熔断
    private static final int FAIL_THRESHOLD = 3;
    // 熔断恢复时间(毫秒)
    private static final long CIRCUIT_BREAKER_RESET_TIME = 30000;
    // 熔断点位的恢复时间记录
    private static final Map<String, Long> CIRCUIT_BREAKER_MAP = new ConcurrentHashMap<>();
    // 格式化工具缓存，避免重复创建
    private static final DecimalFormat DF_2DIGIT = new DecimalFormat("00");
    private static final DecimalFormat DF_4DIGIT = new DecimalFormat("0000");

    private OpcUtil() {
    }






    /**
     * 转换JIVariant值为字符串
     */
    public static String getValue(JIVariant variant) {
        if (variant == null) {
            return null;
        }

        try {
            int type = variant.getType();

            // 布尔类型
            if (type == JIVariant.VT_BOOL) {
                return String.valueOf(variant.getObjectAsBoolean());
            }
            // 字符串类型
            else if (type == JIVariant.VT_BSTR) {
                return variant.getObjectAsString().getString();
            }
            // 无符号整数类型
            else if (type == JIVariant.VT_UI2 || type == JIVariant.VT_UI4) {
                return String.valueOf(variant.getObjectAsUnsigned().getValue());
            }
            // 短整数类型
            else if (type == JIVariant.VT_I2) {
                return String.valueOf(variant.getObjectAsShort());
            }
            // 浮点类型
            else if (type == JIVariant.VT_R4) {
                return String.valueOf(variant.getObjectAsFloat());
            }
            // long数组类型 (8195)
            else if (type == 8195) {
                JIArray jarr = variant.getObjectAsArray();
                Integer[] arr = (Integer[]) jarr.getArrayInstance();

                if (arr == null || arr.length != 6) {
                    logger.warn("long数组格式异常，长度={}", arr == null ? 0 : arr.length);
                    return null;
                }

                // 使用缓存的DecimalFormat提高性能
                return arr[0] + "." + arr[1] + "."
                        + DF_2DIGIT.format(arr[2]) + "."
                        + DF_4DIGIT.format(arr[3]) + "."
                        + arr[4] + "." + arr[5];
            }
            // float数组类型 (8196)
            else if (type == 8196) {
                JIArray jarr = variant.getObjectAsArray();
                Float[] arr = (Float[]) jarr.getArrayInstance();

                if (arr == null || arr.length == 0) {
                    return null;
                }

                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < arr.length; i++) {
                    if (i > 0) {
                        sb.append(',');
                    }
                    sb.append(arr[i]);
                }
                return sb.toString();
            }
            // 空值处理
            else if (type == JIVariant.VT_EMPTY || type == JIVariant.VT_NULL) {
                return null;
            }
            // 其他类型
            else {
                Object value = variant.getObject();
                return value != null ? value.toString() : null;
            }
        } catch (JIException e) {
            logger.error("解析JIVariant值失败", e);
        }
        return null;
    }


}
