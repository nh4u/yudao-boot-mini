package cn.bitlinks.ems.framework.common.util.opcda;

import cn.hutool.core.date.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.jinterop.dcom.common.JIException;
import org.openscada.opc.lib.common.ConnectionInformation;
import org.openscada.opc.lib.da.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * opcda连接管理
 */
@Slf4j
public class OpcDaConnectionManager {

    static class ServerGroupWrapper {
        Server server;
        Group group;
        String groupName; // 新增：存储Group名称，用于连接检测
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        Set<String> currentItems = new HashSet<>();

        // 改造构造函数：新增groupName参数
        ServerGroupWrapper(Server server, Group group, String groupName) {
            this.server = server;
            this.group = group;
            this.groupName = groupName; // 初始化Group名称
        }
    }

    private static final ConcurrentHashMap<String, ServerGroupWrapper> cache = new ConcurrentHashMap<>();
    // 管理每个OPC服务器的 SyncAccess 实例
    public static final Map<String, SyncAccessHolder> SYNC_ACCESS_HOLDER_MAP = new ConcurrentHashMap<>();
    // 专用线程池
    private static final ScheduledExecutorService SYNC_EXECUTOR = Executors.newScheduledThreadPool(
            Runtime.getRuntime().availableProcessors() * 2,
            r -> {
                Thread t = new Thread(r, "opc-sync-executor");
                t.setDaemon(true);
                return t;
            }
    );

    // SyncAccess 持有者
    static class SyncAccessHolder {
        public final SyncAccess syncAccess;
        public final Map<String, ItemStatus> latestResultCache;
        public final Set<String> currentItems;
        public final ReentrantLock updateLock;
        public boolean isRunning;
        // 记录Group中已添加的Item（替代findItem方法）
        public final Map<String, Item> groupItemCache = new ConcurrentHashMap<>();

        public SyncAccessHolder(SyncAccess syncAccess) {
            this.syncAccess = syncAccess;
            this.latestResultCache = new ConcurrentHashMap<>();
            this.currentItems = ConcurrentHashMap.newKeySet();
            this.updateLock = new ReentrantLock();
            this.isRunning = false;
        }
    }

    public static ServerGroupWrapper getOrCreate(String host, String user, String password, String clsid) throws Exception {
        String key = host + "|" + user + "|" + password + "|" + clsid;

        return cache.computeIfAbsent(key, k -> {
            try {
                ConnectionInformation ci = new ConnectionInformation();
                ci.setHost(host);
                ci.setDomain("");
                ci.setUser(user);
                ci.setPassword(password);
                ci.setClsid(clsid);

                ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r ->
                        new Thread(r, "opc-da-" + key.hashCode()));
                Server server = new Server(ci, executor);
                server.connect();

                // 1. 生成并记录Group名称
                String groupName = "LongLivedGroup_" + key.hashCode();
                Group group = server.addGroup(groupName);
                group.setActive(true);

                log.info("Created Server and Group [{}] for key {}", groupName, key);

                // 2. 传入groupName创建ServerGroupWrapper
                return new ServerGroupWrapper(server, group, groupName);
            } catch (Exception e) {
                log.error("Failed to create ServerGroupWrapper for key " + key, e);
                throw new RuntimeException(e);
            }
        });
    }

    // 获取/创建 SyncAccessHolder
    public static SyncAccessHolder getOrCreateSyncAccessHolder(ServerGroupWrapper wrapper, String host, String clsid) throws Exception {
        String key = host + "_" + clsid;
        return SYNC_ACCESS_HOLDER_MAP.computeIfAbsent(key, k -> {
            try {
                SyncAccess syncAccess = new SyncAccess(wrapper.server, 1000);
                SyncAccessHolder holder = new SyncAccessHolder(syncAccess);
                holder.syncAccess.bind();
                holder.isRunning = true;
                return holder;
            } catch (Exception e) {
                throw new RuntimeException("创建 SyncAccess 失败", e);
            }
        });
    }

    // 同步点位到 SyncAccess
    public static void syncItemsToSyncAccess(SyncAccessHolder holder, Group group, List<String> itemList) throws Exception {
        holder.updateLock.lock();
        try {
            Set<String> newItems = new HashSet<>(itemList);
            Set<String> toAdd = new HashSet<>(newItems);
            toAdd.removeAll(holder.currentItems);
            Set<String> toRemove = new HashSet<>(holder.currentItems);
            toRemove.removeAll(newItems);

            // 处理删除点位
            if (!toRemove.isEmpty()) {
                for (String itemId : toRemove) {
                    Item item = holder.groupItemCache.get(itemId);
                    if (item != null) {
                        holder.syncAccess.removeItem(itemId);
                        try {
                            group.removeItem(itemId);
                        } catch (Exception e) {
                            log.warn("点位[{}]从Group中移除失败", itemId, e);
                        }
                        holder.groupItemCache.remove(itemId);
                    }
                    holder.currentItems.remove(itemId);
                    holder.latestResultCache.remove(itemId);
                    log.debug("点位[{}]已从SyncAccess中移除", itemId);
                }
            }

            // 处理新增点位
            if (!toAdd.isEmpty()) {
                try {
                    Map<String, Item> addedItems = group.addItems(toAdd.toArray(new String[0]));
                    for (Map.Entry<String, Item> entry : addedItems.entrySet()) {
                        String itemId = entry.getKey();
                        Item item = entry.getValue();
                        holder.groupItemCache.put(itemId, item);
                        holder.syncAccess.addItem(itemId, (item1, itemState) -> {
                            try {
                                String value = OpcUtil.getValue(itemState.getValue());
                                LocalDateTime time = DateUtil.toLocalDateTime(itemState.getTimestamp());
                                ItemStatus status = new ItemStatus();
                                status.setValue(value);
                                status.setTime(time);
                                holder.latestResultCache.put(itemId, status);
                            } catch (Exception e) {
                                log.error("解析点位[{}]失败", itemId, e);
                                holder.latestResultCache.remove(itemId);
                            }
                        });
                        holder.currentItems.add(itemId);
                        log.debug("点位[{}]已添加到SyncAccess", itemId);
                    }
                } catch (AddFailedException e) {
                    log.warn("部分点位批量添加失败，尝试逐个添加", e);
                    for (String itemId : toAdd) {
                        if (holder.currentItems.contains(itemId)) continue;
                        try {
                            Item item = group.addItem(itemId);
                            holder.groupItemCache.put(itemId, item);
                            holder.syncAccess.addItem(itemId, (item1, itemState) -> {
                                try {
                                    String value = OpcUtil.getValue(itemState.getValue());
                                    LocalDateTime time = DateUtil.toLocalDateTime(itemState.getTimestamp());
                                    ItemStatus status = new ItemStatus();
                                    status.setValue(value);
                                    status.setTime(time);
                                    holder.latestResultCache.put(itemId, status);
                                } catch (Exception ex) {
                                    log.error("解析点位[{}]失败", itemId, ex);
                                    holder.latestResultCache.remove(itemId);
                                }
                            });
                            holder.currentItems.add(itemId);
                        } catch (Exception ex) {
                            log.error("点位[{}]单个添加失败，跳过", itemId, ex);
                        }
                    }
                }
            }
        } finally {
            holder.updateLock.unlock();
        }
    }

    // 从缓存获取最新结果
    public static Map<String, ItemStatus> getLatestResults(SyncAccessHolder holder, List<String> itemList) {
        Map<String, ItemStatus> result = new HashMap<>();
        for (String itemId : itemList) {
            result.put(itemId, holder.latestResultCache.getOrDefault(itemId, new ItemStatus()));
        }
        return result;
    }



}