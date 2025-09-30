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
     * 读一组值，对于读取异常的点位会被过滤或熔断
     */
    public static Map<String, ItemStatus> readValues(Group group, List<String> tags) {
        // 快速校验
        if (group == null || tags == null || tags.isEmpty()) {
            logger.debug("读取参数无效：group={}, tags={}", group, tags);
            return Collections.emptyMap();
        }

        long startTime = System.nanoTime();
        int totalTags = tags.size();

        // 过滤已熔断的点位
        List<String> availableTags = tags.stream()
                .filter(tag -> {
                    Long breakTime = CIRCUIT_BREAKER_MAP.get(tag);
                    return breakTime == null || System.currentTimeMillis() - breakTime > CIRCUIT_BREAKER_RESET_TIME;
                })
                .collect(Collectors.toList());

        if (availableTags.isEmpty()) {
            logPerformance(startTime, totalTags, 0, 0);
            return Collections.emptyMap();
        }

        // 补充缓存中缺失的Item
        List<String> missingTags = availableTags.stream()
                .filter(tag -> !ITEM_CACHE.containsKey(tag))
                .collect(Collectors.toList());

        if (!missingTags.isEmpty()) {
            addMissingItemsToCache(group, missingTags);
        }

        // 收集有效Item
        Map<String, Item> validItems = availableTags.stream()
                .map(tag -> {
                    Item item = ITEM_CACHE.get(tag);
                    return item != null ? new AbstractMap.SimpleEntry<>(tag, item) : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Map<String, ItemStatus> resultMap = new HashMap<>(validItems.size());
        if (validItems.isEmpty()) {
            logPerformance(startTime, totalTags, availableTags.size(), 0);
            return resultMap;
        }

        try {
            // 批量读取
            Item[] itemsArray = validItems.values().toArray(new Item[0]);
            Map<Item, ItemState> itemStateMap = group.read(true, itemsArray);

            // 解析结果
            for (Map.Entry<String, Item> entry : validItems.entrySet()) {
                String tag = entry.getKey();
                Item item = entry.getValue();
                ItemState state = itemStateMap.get(item);

                if (state == null) {
                    handleFailedTag(tag);
                    continue;
                }

                String value = getValue(state.getValue());
                if (value == null) {
                    handleFailedTag(tag);
                    continue;
                }

                // 成功读取，重置失败计数
                FAIL_COUNT_MAP.remove(tag);
                CIRCUIT_BREAKER_MAP.remove(tag);

                ItemStatus status = new ItemStatus();
                status.setValue(value);
                status.setTime(DateUtil.toLocalDateTime(state.getTimestamp()));
                resultMap.put(tag, status);
            }
        } catch (JIException e) {
            logger.error("批量读取OPC点位异常", e);
            // 批量失败时标记所有参与读取的点位
            validItems.keySet().forEach(OpcUtil::handleFailedTag);
        }

        logPerformance(startTime, totalTags, availableTags.size(), resultMap.size());
        return resultMap;
    }

    /**
     * 批量添加缺失的Item到缓存（根据实际addItems方法实现修正）
     */
    private static void addMissingItemsToCache(Group group, List<String> missingTags) {
        if (missingTags.isEmpty()) {
            return;
        }

        try {
            // 调用实际返回Map<String, Item>的addItems方法
            Map<String, Item> addedItems = group.addItems(
                    missingTags.toArray(new String[0])
            );

            // 将成功添加的Item放入缓存
            addedItems.forEach((tag, item) -> {
                ITEM_CACHE.put(tag, item);
                logger.debug("点位[{}]已添加到缓存", tag);
            });

        } catch (AddFailedException e) {
            // 处理部分添加失败的情况
            Map<String, Integer> failedItems = e.getErrors();
            if (failedItems != null && !failedItems.isEmpty()) {
                for (Map.Entry<String, Integer> entry : failedItems.entrySet()) {
                    String tag = entry.getKey();
                    int errorCode = entry.getValue();
                    logger.warn("点位[{}]添加失败，错误码: {}", tag, errorCode);
                    handleFailedTag(tag);
                }
            }

            // 处理成功添加的项目
            Map<String, Item> succeededItems = e.getItems();
            if (succeededItems != null && !succeededItems.isEmpty()) {
                succeededItems.forEach(ITEM_CACHE::put);
                logger.debug("成功添加{}个点位到缓存", succeededItems.size());
            }
        } catch (JIException e) {
            // 处理完全添加失败的情况
            logger.error("批量添加点位到缓存失败", e);
            // 逐个尝试添加
            for (String tag : missingTags) {
                try {
                    Item item = group.addItem(tag);
                    ITEM_CACHE.put(tag, item);
                    logger.debug("点位[{}]已通过单个添加方式加入缓存", tag);
                } catch (Exception ex) {
                    logger.warn("点位[{}]单个添加也失败", tag, ex);
                    handleFailedTag(tag);
                }
            }
        }
    }

    /**
     * 处理失败点位，实现熔断机制
     */
    private static void handleFailedTag(String tag) {
        int failCount = FAIL_COUNT_MAP.getOrDefault(tag, 0) + 1;
        FAIL_COUNT_MAP.put(tag, failCount);

        // 达到阈值触发熔断
        if (failCount >= FAIL_THRESHOLD) {
            CIRCUIT_BREAKER_MAP.put(tag, System.currentTimeMillis());
            logger.warn("点位[{}]连续失败{}次，已熔断，{}秒后重试",
                    tag, failCount, CIRCUIT_BREAKER_RESET_TIME / 1000);
        }
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

    /**
     * 记录性能指标
     */
    private static void logPerformance(long startTime, int totalTags, int activeTags, int successCount) {
        long durationMs = (System.nanoTime() - startTime) / 1_000_000;

        // 仅在延迟过高或调试模式下记录详细日志
        if (durationMs > 100) { // 超过100ms视为延迟过高
            logger.warn("OPC读取延迟过高: {}ms, 总点位: {}, 活跃点位: {}, 成功: {}",
                    durationMs, totalTags, activeTags, successCount);
        } else if (logger.isDebugEnabled()) {
            logger.debug("OPC读取完成: 耗时{}ms, 总点位: {}, 活跃点位: {}, 成功: {}",
                    durationMs, totalTags, activeTags, successCount);
        }
    }

    /**
     * 清理缓存，在连接关闭时调用
     */
    public static void clearCache() {
        ITEM_CACHE.clear();
        FAIL_COUNT_MAP.clear();
        CIRCUIT_BREAKER_MAP.clear();
        logger.info("OPC缓存已清理");
    }

}
