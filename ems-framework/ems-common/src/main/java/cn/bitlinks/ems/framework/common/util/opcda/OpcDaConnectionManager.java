package cn.bitlinks.ems.framework.common.util.opcda;

import lombok.extern.slf4j.Slf4j;
import org.openscada.opc.lib.common.ConnectionInformation;
import org.openscada.opc.lib.da.Server;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * opcda连接管理
 */
@Slf4j
public class OpcDaConnectionManager {

    private static final ConcurrentHashMap<String, Server> serverMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ScheduledExecutorService> executorMap = new ConcurrentHashMap<>();

    public static Server getServer(String host, String user, String password, String clsid) throws Exception {
        String key = host + "_" + clsid;
        if (!serverMap.containsKey(key)) {
            synchronized (OpcDaConnectionManager.class) {
                if (!serverMap.containsKey(key)) {
                    ConnectionInformation ci = new ConnectionInformation();
                    ci.setHost(host);
                    ci.setDomain("");
                    ci.setUser(user);
                    ci.setPassword(password);
                    ci.setClsid(clsid);

                    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
                    Server server = new Server(ci, executor);
                    server.connect();

                    serverMap.put(key, server);
                    executorMap.put(key, executor);
                }
            }
        }
        return serverMap.get(key);
    }

    public static void disconnectAll() {
        for (String key : serverMap.keySet()) {
            try {
                serverMap.get(key).disconnect();
                executorMap.get(key).shutdown();
            } catch (Exception e) {
                // 记录日志或处理异常
                log.error("Failed to disconnect OPC DA server: {}", key, e);
            }
        }
        serverMap.clear();
        executorMap.clear();
    }
}
