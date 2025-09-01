package cn.bitlinks.ems.framework.common.util.opcda;

import lombok.extern.slf4j.Slf4j;
import org.openscada.opc.lib.common.ConnectionInformation;
import org.openscada.opc.lib.da.Group;
import org.openscada.opc.lib.da.Server;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * opcda连接管理
 */
@Slf4j
public class OpcDaConnectionManager {

    static class ServerGroupWrapper {
        Server server;
        Group group;
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        Set<String> currentItems = new HashSet<>();

        ServerGroupWrapper(Server server, Group group) {
            this.server = server;
            this.group = group;
        }
    }

    private static final ConcurrentHashMap<String, ServerGroupWrapper> cache = new ConcurrentHashMap<>();

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
                Group group = server.addGroup("LongLivedGroup_" + key.hashCode());
                group.setActive(true);
                log.info("Created Server and Group for key {}", key);
                return new ServerGroupWrapper(server, group);
            } catch (Exception e) {
                log.error("Failed to create ServerGroupWrapper for key " + key, e);
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * 批量更新group items，线程安全
     */
    public static void updateGroupItems(ServerGroupWrapper wrapper, Set<String> newItems) throws Exception {
        wrapper.lock.writeLock().lock();
        try {
            Set<String> toRemove = new HashSet<>(wrapper.currentItems);
            toRemove.removeAll(newItems);

            Set<String> toAdd = new HashSet<>(newItems);
            toAdd.removeAll(wrapper.currentItems);

            if (!toRemove.isEmpty()) {
                wrapper.group.clear();
                log.debug("Removed items: {}", toRemove);
            }

            if (!toAdd.isEmpty()) {
                wrapper.group.addItems(toAdd.toArray(new String[0]));
                log.debug("Added items: {}", toAdd);
            }

            wrapper.currentItems.clear();
            wrapper.currentItems.addAll(newItems);
        } finally {
            wrapper.lock.writeLock().unlock();
        }
    }

    /**
     * 读取items数据，线程安全
     */
    public static Map<String, ItemStatus> readItems(ServerGroupWrapper wrapper, List<String> items) throws Exception {
        wrapper.lock.readLock().lock();
        try {
            return OpcUtil.readValues(wrapper.group, items);
        } finally {
            wrapper.lock.readLock().unlock();
        }
    }

    /**
     * 关闭所有server和group
     */
    public static void closeAll() {
        cache.values().forEach(wrapper -> {
            try {
                wrapper.server.removeGroup(wrapper.group, true);
            } catch (Exception e) {
                log.warn("Failed to remove group", e);
            }
            try {
                wrapper.server.disconnect();
            } catch (Exception e) {
                log.warn("Failed to disconnect server", e);
            }
        });
        cache.clear();
    }
}
