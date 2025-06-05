package cn.bitlinks.ems.framework.common.util.opcda;

import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * opcda 连接监控
 */
@Slf4j
public class OpcDaConnectionMonitor {

    private static final ScheduledExecutorService monitorPool = Executors.newScheduledThreadPool(1);
    private static final long MONITOR_INTERVAL = 30;
    private static final Set<String> monitoredKeys = ConcurrentHashMap.newKeySet();

    public static void startMonitoring(String host, String user, String password, String clsid) {
        String key = host + "|" + user + "|" + password + "|" + clsid;

        if (!monitoredKeys.add(key)) {
            // 已经监控中
            return;
        }

        monitorPool.scheduleAtFixedRate(() -> {
            try {
                OpcDaConnectionManager.ServerGroupWrapper wrapper =
                        OpcDaConnectionManager.getOrCreate(host, user, password, clsid);

                if (!wrapper.server.isDefaultActive()) {
                    log.warn("OPC Server [{}] not connected. Trying to reconnect...", key);
                    wrapper.server.connect();
                } else {
                    log.debug("OPC Server [{}] is alive", key);
                }
            } catch (Exception e) {
                log.error("Failed to monitor OPC Server [{}]", key, e);
            }
        }, 5, MONITOR_INTERVAL, TimeUnit.SECONDS);

        log.info("Started OPC monitoring for [{}]", key);
    }

    public static void shutdown() {
        monitorPool.shutdownNow();
        monitoredKeys.clear();
    }
}