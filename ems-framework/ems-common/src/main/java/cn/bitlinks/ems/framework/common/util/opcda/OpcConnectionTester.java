package cn.bitlinks.ems.framework.common.util.opcda;

import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.jinterop.dcom.common.JIException;
import org.openscada.opc.lib.common.ConnectionInformation;
import org.openscada.opc.lib.da.AddFailedException;
import org.openscada.opc.lib.da.Group;
import org.openscada.opc.lib.da.Item;
import org.openscada.opc.lib.da.Server;

import java.util.Collections;
import java.util.List;
import java.util.Map;
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

    /**
     * 测试链接读取值
     *
     * @param host
     * @param user
     * @param password
     * @param clsid
     * @param itemList
     * @return
     */
    public static Map<String, ItemStatus> testLink(String host, String user, String password, String clsid,
                                                   List<String> itemList) {
        if (CollUtil.isEmpty(itemList)) {
            return Collections.emptyMap();
        }

        String key = host + "|" + user + "|" + password + "|" + clsid;
        String[] items = itemList.toArray(new String[0]);

        Server server = null;
        Group group = null;
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        try {
            ConnectionInformation ci = new ConnectionInformation();
            ci.setHost(host);
            ci.setDomain("");
            ci.setUser(user);
            ci.setPassword(password);
            ci.setClsid(clsid);

            server = new Server(ci, executor);
            server.connect();

            group = server.addGroup("OneTest_" + key.hashCode());
            group.setActive(true);

            // 等待 OPC Server 处理完 group 初始化（某些厂商需要）
            Thread.sleep(50);

            // ✅ 手动添加所有点位到 group（关键步骤！） 在调用 OpcUtil.readValues(...) 之前，确保点位已添加到 group
            try {
                Map<String, Item> addedItems = group.addItems(items);
                log.info("成功添加 {} 个点位到 group", addedItems.size());
            } catch (AddFailedException e) {
                log.error("批量添加点位失败，错误: {}", e.getMessage());
                // 可以继续尝试读取，但可能部分点位读取失败
            } catch (JIException e) {
                log.error("添加点位异常: {}", e.getMessage());
            }

            return OpcUtil.readValues(group, itemList);

        } catch (Exception e) {
            log.error("Failed to read values from OPC server", e);
            return null;
        } finally {
            try {
                if (group != null) {
                    server.removeGroup(group, true);
                }
            } catch (Exception ex) {
                log.warn("释放 OPC group 失败", ex);
            }
            try {
                if (server != null) {
                    server.disconnect();
                }
            } catch (Exception ex) {
                log.warn("断开 OPC server 失败", ex);
            }
            executor.shutdownNow(); // 非常重要
        }
    }

    private static boolean retryOperation(RetryableOperation operation, int retryCount) {
        for (int i = 0; i <= retryCount-1; i++) {
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
        log.error("OPC连接测试失败，重试 {} 次后仍失败", retryCount);
        return false;
    }

    @FunctionalInterface
    private interface RetryableOperation {
        boolean execute() throws InterruptedException;
    }
}