package cn.bitlinks.ems.framework.common.util.opcda;

import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * opc-da协议 获取数据
 */
@Slf4j
public class OpcDaUtils {

    /**
     * 批量更新 items 并读取值（线程安全，内部加锁）
     *
     * @param host     OPC host
     * @param user     用户名
     * @param password 密码
     * @param clsid    OPC CLSID
     * @param itemList 要读取的 Items
     * @return item -> status 映射，失败返回空 map
     */
    public static Map<String, ItemStatus> batchGetValue(String host, String user, String password, String clsid,
                                                        List<String> itemList) {
        if (CollUtil.isEmpty(itemList)) {
            return Collections.emptyMap();
        }

        try {
            // 连接 + 缓存 + 创建 group
            OpcDaConnectionManager.ServerGroupWrapper wrapper =
                    OpcDaConnectionManager.getOrCreate(host, user, password, clsid);

            // 自动启动连接监控
            OpcDaConnectionMonitor.startMonitoring(host, user, password, clsid);

            // 更新 items（带线程锁）
            OpcDaConnectionManager.updateGroupItems(wrapper, new HashSet<>(itemList));

            // 读取数据
            return OpcDaConnectionManager.readItems(wrapper, itemList);
        } catch (Exception e) {
            log.error("OPC updateItemsAndRead 失败 [host={}, clsid={}, items={}]", host, clsid, itemList, e);
            return Collections.emptyMap();
        }
    }

    public static Map<String, ItemStatus> readOnly(String host, String user, String password, String clsid,
                                                   List<String> itemList) {
        if (CollUtil.isEmpty(itemList)) {
            return Collections.emptyMap();
        }

        try {
            OpcDaConnectionManager.ServerGroupWrapper wrapper =
                    OpcDaConnectionManager.getOrCreate(host, user, password, clsid);

            // 启动监控
            OpcDaConnectionMonitor.startMonitoring(host, user, password, clsid);

            return OpcDaConnectionManager.readItems(wrapper, itemList);
        } catch (Exception e) {
            log.error("OPC readOnly 失败 [host={}, clsid={}, items={}]", host, clsid, itemList, e);
            return Collections.emptyMap();
        }
    }
}
