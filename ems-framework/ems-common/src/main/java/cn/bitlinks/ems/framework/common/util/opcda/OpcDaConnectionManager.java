package cn.bitlinks.ems.framework.common.util.opcda;

import lombok.extern.slf4j.Slf4j;
import org.jinterop.dcom.common.JIException;
import org.openscada.opc.lib.common.ConnectionInformation;
import org.openscada.opc.lib.da.Group;
import org.openscada.opc.lib.da.Server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReentrantLock;

/**
 * opcda连接管理
 */
@Slf4j
public class OpcDaConnectionManager {

    static class ServerGroupWrapper {
        private final Server server;
        private final Group group;
        private final ScheduledExecutorService executor;

        public ServerGroupWrapper(Server server, Group group, ScheduledExecutorService executor) {
            this.server = server;
            this.group = group;
            this.executor = executor;
        }

        public Server getServer() {
            return server;
        }

        public Group getGroup() {
            return group;
        }

        // 关闭资源
        public void close() {
            try {
                server.disconnect();
            } catch (Exception e) {
                log.warn("断开连接失败", e);
            }
            executor.shutdownNow();
        }
    }

    // 缓存服务器连接和组，使用ConcurrentHashMap保证线程安全
    private final Map<String, ServerGroupWrapper> cache = new ConcurrentHashMap<>();
    // 为每个服务器连接添加锁，防止并发操作冲突
    private final Map<String, ReentrantLock> connectionLocks = new ConcurrentHashMap<>();

    /**
     * 获取或创建服务器连接和组，添加同步控制防止并发冲突
     */
    public ServerGroupWrapper getOrCreate(String host, String user, String password, String clsid) throws Exception {
        String key = host + "|" + user + "|" + password + "|" + clsid;

        // 为每个连接创建独立的锁，避免不同服务器间的锁竞争
        ReentrantLock lock = connectionLocks.computeIfAbsent(key, k -> new ReentrantLock());

        try {
            // 加锁保证同一服务器连接的操作串行执行
            lock.lock();

            // 双重检查，避免重复创建
            if (cache.containsKey(key)) {
                ServerGroupWrapper wrapper = cache.get(key);
                // 验证连接是否有效，无效则重建
                if (isConnectionValid(wrapper)) {
                    return wrapper;
                } else {
                    log.warn("连接 {} 已失效，将重建", key);
                    cache.remove(key);
                }
            }

            // 创建新的连接和组
            return createNewServerGroup(key, host, user, password, clsid);
        } finally {
            // 确保锁释放
            lock.unlock();
        }
    }

    /**
     * 创建新的服务器连接和组，增加详细的错误处理
     */
    private ServerGroupWrapper createNewServerGroup(String key, String host, String user, String password, String clsid) throws Exception {
        ConnectionInformation ci = new ConnectionInformation();
        ci.setHost(host);
        ci.setDomain("");
        ci.setUser(user);
        ci.setPassword(password);
        ci.setClsid(clsid);

        // 使用带有线程工厂的线程池，便于问题追踪
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "opc-da-" + key.hashCode());
            thread.setDaemon(true); // 设置为守护线程，避免程序无法退出
            return thread;
        });

        Server server = null;
        Group group = null;
        try {
            server = new Server(ci, executor);
            server.connect();

            // 增加组创建的重试机制
            int retryCount = 3;
            while (retryCount-- > 0) {
                try {
                    // 为组设置合理的参数
                    group = server.addGroup("LongLivedGroup_" + key.hashCode());
                    group.setActive(true);
                    break;
                } catch (JIException e) {
                    log.error("创建组失败，剩余重试次数: {}，错误: {}", retryCount, e.getMessage());
                    if (retryCount == 0) throw e;
                    Thread.sleep(100);
                }
            }

            log.info("成功创建服务器连接和组: {}", key);
            ServerGroupWrapper wrapper = new ServerGroupWrapper(server, group, executor);
            cache.put(key, wrapper);
            return wrapper;
        } catch (Exception e) {
            log.error("创建服务器连接和组失败: {}", key, e);
            // 发生异常时确保资源释放
            if (group != null) {
                try {
                    server.removeGroup(group, true);
                } catch (Exception ex) {
                }
            }
            if (server != null) {
                try {
                    server.disconnect();
                } catch (Exception ex) {
                }
            }
            executor.shutdownNow();
            throw e;
        }
    }

    /**
     * 验证连接是否有效
     */
    private boolean isConnectionValid(ServerGroupWrapper wrapper) {
        try {
            // 通过简单操作验证连接状态
            wrapper.getServer().getServerState();
            // 补充验证组是否激活
            if (!wrapper.getGroup().isActive()) {
                log.warn("组未激活，连接视为无效");
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 关闭指定服务器连接
     */
    public void closeConnection(String host, String user, String password, String clsid) {
        String key = host + "|" + user + "|" + password + "|" + clsid;
        ReentrantLock lock = connectionLocks.get(key);
        if (lock != null) lock.lock();
        try {
            ServerGroupWrapper wrapper = cache.remove(key);
            if (wrapper != null) {
                wrapper.close();
                log.info("关闭服务器连接: {}", key);
            }
        } finally {
            if (lock != null) lock.unlock();
            connectionLocks.remove(key);
        }
    }

}
