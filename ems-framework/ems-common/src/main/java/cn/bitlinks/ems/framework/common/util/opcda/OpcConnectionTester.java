package cn.bitlinks.ems.framework.common.util.opcda;

import lombok.extern.slf4j.Slf4j;
import org.openscada.opc.lib.common.ConnectionInformation;
import org.openscada.opc.lib.da.Server;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * 测试opc-da连接
 */
@Slf4j
public class OpcConnectionTester {

    /**
     * 测试连接OPC服务器，带重试机制。
     *
     * @param host       电脑IP
     * @param user       电脑上自己建好的用户名
     * @param password   用户名的密码
     * @param clsid      KEPServer 的注册表ID
     * @param retryCount 重试次数
     * @return true 如果连接成功，否则 false。
     */
    public static boolean testLink(String host, String user, String password, String clsid, int retryCount) {
        ConnectionInformation ci = new ConnectionInformation();
        ci.setHost(host);
        ci.setDomain("");
        ci.setUser(user);
        ci.setPassword(password);
        ci.setClsid(clsid);

        return retryOperation(() -> testOpcConnection(ci), retryCount);
    }

    /**
     * 测试连接
     *
     * @param ci 连接信息
     * @return
     */
    public static boolean testOpcConnection(ConnectionInformation ci) {
        ScheduledExecutorService executor = null;
        Server server = null;
        try {
            executor = Executors.newSingleThreadScheduledExecutor();
            server = new Server(ci, executor);
            server.connect();
            return true;
        } catch (Exception e) {
            log.error("测试连接时发生异常", e);
            return false;
        } finally {
            if (server != null) {
                server.disconnect();
            }
            if (executor != null) {
                executor.shutdown();
            }
        }
    }

    private static boolean retryOperation(RetryableOperation operation, int retryCount) {
        for (int i = 0; i <= retryCount; i++) {
            try {
                if (operation.execute()) {
                    log.info("OPC连接测试成功 (第 {} 次尝试)", i + 1);
                    return true;
                } else {
                    log.warn("OPC连接测试失败 (第 {} 次尝试)", i + 1);
                    if (i < retryCount) {
                        Thread.sleep(1000);
                    }
                }
            } catch (InterruptedException e) {
                log.warn("线程在休眠时被中断", e);
                Thread.currentThread().interrupt();
                return false;
            }
        }
        log.error("OPC连接测试失败，重试 {} 次后仍失败", retryCount + 1);
        return false;
    }

    @FunctionalInterface
    private interface RetryableOperation {
        boolean execute() throws InterruptedException;
    }
}