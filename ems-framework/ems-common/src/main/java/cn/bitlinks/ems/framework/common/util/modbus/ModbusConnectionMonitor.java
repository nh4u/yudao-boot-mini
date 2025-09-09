package cn.bitlinks.ems.framework.common.util.modbus;

import com.ghgande.j2mod.modbus.net.TCPMasterConnection;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ModbusConnectionMonitor 监控 Modbus 连接的健康状态
 */
@Slf4j
public class ModbusConnectionMonitor {

    private static final long MONITOR_INTERVAL = 5000L; // 监控间隔时间（毫秒）
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private final TCPMasterConnection connection;
    private final String host;
    private final int port;
    private final String registerType;
    private final String salveAddr;
    private final ReentrantLock lock = new ReentrantLock();

    private boolean connected = false;

    public ModbusConnectionMonitor(String host, int port, String registerType, String salveAddr) {
        this.host = host;
        this.port = port;
        this.registerType = registerType;
        this.salveAddr = salveAddr;
        this.connection = createConnection(host, port);
    }

    /**
     * 创建 Modbus 连接
     */
    private TCPMasterConnection createConnection(String host, int port) {
        try {
            InetAddress addr = InetAddress.getByName(host);
            TCPMasterConnection connection = new TCPMasterConnection(addr);
            connection.setPort(port);
            return connection;
        } catch (Exception e) {
            log.error("Failed to create Modbus connection", e);
            return null;
        }
    }

    /**
     * 启动连接监控
     */
    public void startMonitoring() {
        Runnable monitorTask = () -> {
            try {
                lock.lock();
                if (connected) {
                    if (!connection.isConnected()) {
                        log.warn("Modbus connection lost, attempting to reconnect...");
                        reconnect();
                    }
                } else {
                    log.info("Modbus connection not established yet, trying to connect...");
                    connect();
                }
            } catch (Exception e) {
                log.error("Error during Modbus connection monitoring", e);
            } finally {
                lock.unlock();
            }
        };

        // 定期执行监控任务
        scheduler.scheduleAtFixedRate(monitorTask, 0, MONITOR_INTERVAL, TimeUnit.MILLISECONDS);
    }

    /**
     * 尝试连接 Modbus 设备
     */
    private void connect() {
        try {
            connection.connect();
            connected = true;
            log.info("Modbus connection established");
        } catch (Exception e) {
            log.error("Failed to connect to Modbus device", e);
        }
    }

    /**
     * 重新连接 Modbus 设备
     */
    private void reconnect() {
        try {
            connection.close(); // 先关闭旧的连接
            connection.connect(); // 然后重新连接
            connected = true;
            log.info("Modbus connection re-established");
        } catch (Exception e) {
            log.error("Failed to reconnect to Modbus device", e);
        }
    }
}
