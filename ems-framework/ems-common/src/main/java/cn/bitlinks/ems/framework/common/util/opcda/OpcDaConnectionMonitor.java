package cn.bitlinks.ems.framework.common.util.opcda;

import lombok.extern.slf4j.Slf4j;
import org.openscada.opc.lib.common.ConnectionInformation;
import org.openscada.opc.lib.da.Group;
import org.openscada.opc.lib.da.Server;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * opcda 连接监控（支持自动重连+轮询恢复）
 */
@Slf4j
public class OpcDaConnectionMonitor {

    private static final ScheduledExecutorService monitorPool = Executors.newScheduledThreadPool(
            Runtime.getRuntime().availableProcessors(),
            r -> {
                Thread t = new Thread(r, "opc-monitor-thread");
                t.setDaemon(true);
                return t;
            }
    );
    private static final long MONITOR_INTERVAL = 30; // 监测周期：30秒
    private static final Set<String> monitoredKeys = ConcurrentHashMap.newKeySet();

    /**
     * 启动对指定OPC服务器的监控
     */
    public static void startMonitoring(String host, String user, String password, String clsid) {
        String key = host + "|" + user + "|" + password + "|" + clsid;
        if (!monitoredKeys.add(key)) {
            log.debug("OPC服务器[{}]已在监控中，无需重复启动", key);
            return;
        }

        monitorPool.scheduleAtFixedRate(() -> {
            try {
                OpcDaConnectionManager.ServerGroupWrapper wrapper =
                        OpcDaConnectionManager.getOrCreate(host, user, password, clsid);

                // 修正：传入 wrapper.groupName（ServerGroupWrapper 已记录的Group名称）
                if (!isServerValid(wrapper.server, wrapper.groupName)) {
                    log.error("OPC服务器[{}]连接失效，开始自动恢复", key);
                    restoreServerAndSync(wrapper, host, user, password, clsid, key);
                    log.info("OPC服务器[{}]自动恢复完成", key);
                } else {
                    log.debug("OPC服务器[{}]连接正常", key);
                }
            } catch (Exception e) {
                log.error("OPC服务器[{}]监控任务执行失败", key, e);
            }
        }, 5, MONITOR_INTERVAL, TimeUnit.SECONDS);

        log.info("OPC服务器[{}]监控任务已启动（监测周期：{}秒）", key, MONITOR_INTERVAL);
    }

    /**
     * 检测Server连接是否有效（使用已知Group名称）
     */
    private static boolean isServerValid(Server server, String groupName) {
        if (server == null || groupName == null) {
            return false;
        }
        try {
            // 1. 检查基础连接状态
            if (!server.isDefaultActive()) {
                return false;
            }
            // 2. 尝试查找已知名称的Group（替代getGroups()）
            Group group = server.findGroup(groupName);
            // 3. 确保Group处于激活状态
            return group != null && group.isActive();
        } catch (Exception e) {
            log.warn("Server连接无效，Group查找失败", e);
            return false;
        }
    }

    /**
     * 完整恢复逻辑：重连Server + 重建Group + 恢复SyncAccess轮询
     */
    private static void restoreServerAndSync(
            OpcDaConnectionManager.ServerGroupWrapper wrapper,
            String host, String user, String password, String clsid,
            String key) throws Exception {
        wrapper.lock.writeLock().lock();
        try {
            // 1. 重连Server
            if (wrapper.server != null) {
                try {
                    wrapper.server.disconnect();
                    log.debug("OPC服务器[{}]旧连接已关闭", key);
                } catch (Exception e) {
                    log.warn("关闭旧连接失败，忽略继续", e);
                }
            }
            Server newServer = createNewServer(host, user, password, clsid, key);
            wrapper.server = newServer;

            // 2. 重建Group并恢复点位
            if (wrapper.group != null) {
                try {
                    wrapper.server.removeGroup(wrapper.group, true);
                } catch (Exception e) {
                    log.warn("移除旧Group失败，忽略继续", e);
                }
            }
            Group newGroup = wrapper.server.addGroup("LongLivedGroup_" + key.hashCode());
            newGroup.setActive(true);
            if (!wrapper.currentItems.isEmpty()) {
                newGroup.addItems(wrapper.currentItems.toArray(new String[0]));
                log.debug("已恢复点位：{}个", wrapper.currentItems.size());
            }
            wrapper.group = newGroup;

            // 3. 恢复SyncAccess轮询
            String syncKey = host + "_" + clsid;
            OpcDaConnectionManager.SyncAccessHolder syncHolder =
                    OpcDaConnectionManager.SYNC_ACCESS_HOLDER_MAP.get(syncKey);
            if (syncHolder != null && syncHolder.isRunning) {
                syncHolder.syncAccess.unbind();
                syncHolder.groupItemCache.clear();
                OpcDaConnectionManager.syncItemsToSyncAccess(
                        syncHolder, newGroup, new ArrayList<>(wrapper.currentItems)
                );
                syncHolder.syncAccess.bind();
                log.debug("SyncAccess轮询已恢复");
            }

        } finally {
            wrapper.lock.writeLock().unlock();
        }
    }

    /**
     * 新建Server实例
     */
    private static Server createNewServer(String host, String user, String password, String clsid, String key) throws Exception {
        ConnectionInformation ci = new ConnectionInformation();
        ci.setHost(host);
        ci.setDomain("");
        ci.setUser(user);
        ci.setPassword(password);
        ci.setClsid(clsid);
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(
                r -> new Thread(r, "opc-da-" + key.hashCode())
        );
        Server newServer = new Server(ci, executor);
        newServer.connect();
        return newServer;
    }

    /**
     * 关闭监控线程池
     */
    public static void shutdown() {
        monitorPool.shutdownNow();
        monitoredKeys.clear();
        log.info("OPC连接监控池已关闭");
    }
}