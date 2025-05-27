package cn.bitlinks.ems.framework.common.util.opcda;

import lombok.extern.slf4j.Slf4j;
import org.openscada.opc.lib.da.Server;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * opcda 连接监控
 */
@Slf4j
public class OpcDaConnectionMonitor {

    private static final ScheduledExecutorService monitorExecutor = Executors.newSingleThreadScheduledExecutor();

    public static void startMonitoring(String host, String user, String password, String clsid) {
        monitorExecutor.scheduleAtFixedRate(() -> {
            try {
                Server server = OpcDaConnectionManager.getServer(host, user, password, clsid);
                if (!server.isDefaultActive()) {
                    server.connect();
                }
            } catch (Exception e) {
                log.error("OPC DA server 重连失败: {}", host + "_" + clsid, e);
            }
        }, 0, 30, TimeUnit.SECONDS); // 每30秒检查一次连接状态
    }

    public static void stopMonitoring() {
        monitorExecutor.shutdown();
    }
}
