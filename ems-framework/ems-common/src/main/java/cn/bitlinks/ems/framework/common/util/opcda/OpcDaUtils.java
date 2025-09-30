package cn.bitlinks.ems.framework.common.util.opcda;

import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * opc-da协议 获取数据
 */
@Slf4j
public class OpcDaUtils {



    public static Map<String, ItemStatus> readOnly(String host, String user, String password, String clsid,
                                                   List<String> itemList) {
        if (CollUtil.isEmpty(itemList)) {
            return Collections.emptyMap();
        }

        try {
            // 1. 复用现有连接：获取 Server + Group
            OpcDaConnectionManager.ServerGroupWrapper wrapper =
                    OpcDaConnectionManager.getOrCreate(host, user, password, clsid);

            // 2. 启动监控（原有逻辑保留，确保连接状态）
            OpcDaConnectionMonitor.startMonitoring(host, user, password, clsid);

            // 3. 获取/创建 SyncAccess 实例（确保持续轮询）
            OpcDaConnectionManager.SyncAccessHolder syncHolder =
                    OpcDaConnectionManager.getOrCreateSyncAccessHolder(wrapper, host, clsid);

            // 4. 同步点位：确保 SyncAccess 监控的点位与传入的 itemList 一致
            OpcDaConnectionManager.syncItemsToSyncAccess(syncHolder, wrapper.group, itemList);

            // 5. 从缓存获取最新结果（替代原 readItems 单次读取）
            return OpcDaConnectionManager.getLatestResults(syncHolder, itemList);

        } catch (Exception e) {
            log.error("OPC readOnly 失败 [host={}, clsid={}, items={}]", host, clsid, itemList, e);
            return Collections.emptyMap();
        }
    }
}
