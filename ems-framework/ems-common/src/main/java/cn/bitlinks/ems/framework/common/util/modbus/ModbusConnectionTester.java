package cn.bitlinks.ems.framework.common.util.modbus;

import cn.bitlinks.ems.framework.common.enums.RegisterTypeEnum;
import cn.bitlinks.ems.framework.common.util.opcda.ItemStatus;
import cn.hutool.core.collection.CollUtil;
import com.ghgande.j2mod.modbus.io.ModbusTCPTransaction;
import com.ghgande.j2mod.modbus.msg.*;
import com.ghgande.j2mod.modbus.net.TCPMasterConnection;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.bitlinks.ems.framework.common.util.modbus.ModbusConnectionManager.createModbusRequest;
import static cn.bitlinks.ems.framework.common.util.modbus.ModbusConnectionManager.parseResponse;

/**
 * 测试modbus连接
 */
@Slf4j
public class ModbusConnectionTester {

    /**
     * 测试连接OPC服务器，带重试机制。
     *
     * @param host       电脑IP
     * @param port       端口
     * @param retryCount 重试次数
     * @return true 如果连接成功，否则 false。
     */
    public static boolean testLink(String host, Integer port, int retryCount) {
        return retryOperation(() -> testModbusConnection(host, port), retryCount);
    }

    /**
     * 测试连接
     *
     * @param host ip
     * @param port 端口
     * @return
     */
    public static boolean testModbusConnection(String host, int port) {
        TCPMasterConnection connection = null;
        try {
            InetAddress addr = InetAddress.getByName(host);
            connection = new TCPMasterConnection(addr);
            connection.setPort(port);
            connection.connect();
            return true;
        } catch (Exception e) {
            log.error("测试 Modbus 连接时发生异常", e);
            return false;
        } finally {
            if (connection != null && connection.isConnected()) {
                connection.close();
            }
        }
    }

    /**
     * 测试链接读取值
     *
     * @param host
     * @param port
     * @param registerType 寄存器类型
     * @param salveAddr    从地址
     * @param itemList     逻辑地址列表
     * @return
     */
    public static Map<String, ItemStatus> testLink(String host, Integer port, String registerType, String salveAddr,
                                                   List<String> itemList) {
        if (CollUtil.isEmpty(itemList)) {
            return Collections.emptyMap();
        }

        // 创建返回结果的 map
        Map<String, ItemStatus> resultMap = new HashMap<>();

        // 遍历每个 item（寄存器地址）
        for (String item : itemList) {
            try {
                int registerAddress = Integer.parseInt(item); // 转换 item 为寄存器地址

                // 创建 Modbus 请求，根据寄存器类型选择请求类型
                ModbusRequest request = createModbusRequest(RegisterTypeEnum.codeOf(registerType), registerAddress);

                if (request == null) {
                    log.error("无效的寄存器类型: {}", registerType);
                    continue;  // 如果寄存器类型无效，跳过该项
                }

                // 设置 UnitID（设备地址）
                request.setUnitID(Long.valueOf(salveAddr).intValue());

                // 创建 Modbus 连接和事务
                InetAddress addr = InetAddress.getByName(host);
                TCPMasterConnection connection = new TCPMasterConnection(addr);
                connection.setPort(port);
                connection.connect();

                ModbusTCPTransaction transaction = new ModbusTCPTransaction(connection);
                transaction.setRequest(request);

                // 执行事务
                transaction.execute();

                // 获取响应数据
                ModbusResponse response = transaction.getResponse();

                // 读取寄存器值并构造 ItemStatus
                ItemStatus itemStatus = parseResponse(response, item);

                // 将 itemStatus 放入 resultMap，使用 item 为 key
                resultMap.put(item, itemStatus);

                // 关闭连接
                connection.close();

            } catch (Exception e) {
                // 捕获每个 item 的异常并记录错误
                log.error("读取寄存器 {} 时出错", item, e);
            }
        }

// 返回最终的 resultMap
        return resultMap;
    }

    private static boolean retryOperation(RetryableOperation operation, int retryCount) {
        for (int i = 0; i <= retryCount - 1; i++) {
            try {
                if (operation.execute()) {
                    log.info("modbus连接测试成功 (第 {} 次尝试)", i + 1);
                    return true;
                } else {
                    log.warn("modbus连接测试失败 (第 {} 次尝试)", i + 1);
                    if (i < retryCount) {
                        Thread.sleep(1000);
                    }
                }
            } catch (InterruptedException e) {
                log.warn("modbus线程在休眠时被中断", e);
                Thread.currentThread().interrupt();
                return false;
            }
        }
        log.error("modbus连接测试失败，重试 {} 次后仍失败", retryCount);
        return false;
    }

    @FunctionalInterface
    private interface RetryableOperation {
        boolean execute() throws InterruptedException;
    }
}