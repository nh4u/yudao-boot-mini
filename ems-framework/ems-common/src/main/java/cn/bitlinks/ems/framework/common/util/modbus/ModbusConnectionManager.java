package cn.bitlinks.ems.framework.common.util.modbus;

import cn.bitlinks.ems.framework.common.enums.RegisterTypeEnum;
import cn.bitlinks.ems.framework.common.util.opcda.ItemStatus;
import com.ghgande.j2mod.modbus.io.ModbusTCPTransaction;
import com.ghgande.j2mod.modbus.msg.*;
import com.ghgande.j2mod.modbus.net.TCPMasterConnection;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * ModbusConnectionManager 管理 Modbus 连接和数据读取
 */
@Slf4j
public class ModbusConnectionManager {

    static class ConnectionWrapper {
        TCPMasterConnection connection;
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        Set<String> currentItems = new HashSet<>();

        ConnectionWrapper(TCPMasterConnection connection) {
            this.connection = connection;
        }
    }

    private static final Map<String, ConnectionWrapper> cache = new HashMap<>();

    /**
     * 获取或创建 Modbus 连接
     *
     * @param host         目标主机 IP
     * @param port         目标端口
     * @param registerType 寄存器类型
     * @param salveAddr    从站地址
     * @return 返回连接包装器
     */
    public static ConnectionWrapper getOrCreate(String host, String port, String registerType, String salveAddr) throws Exception {
        String key = host + "|" + port + "|" + registerType + "|" + salveAddr;

        return cache.computeIfAbsent(key, k -> {
            try {
                // 创建 Modbus 连接
                InetAddress addr = InetAddress.getByName(host);
                TCPMasterConnection connection = new TCPMasterConnection(addr);
                connection.setPort(Integer.parseInt(port));
                connection.connect();

                log.info("Created Modbus TCP Connection for key {}", key);
                return new ConnectionWrapper(connection);
            } catch (Exception e) {
                log.error("Failed to create Modbus TCP ConnectionWrapper for key " + key, e);
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * 通过 Modbus 连接读取多个寄存器值
     *
     * @param host         目标主机
     * @param port         目标端口
     * @param registerType 寄存器类型
     * @param salveAddr    从站地址
     * @param itemList     寄存器地址列表
     * @return 返回寄存器的值
     */
    public static Map<String, ItemStatus> readRegisters(String host, Integer port, String registerType, String salveAddr,
                                                        List<String> itemList) {
        if (itemList == null || itemList.isEmpty()) {
            return Collections.emptyMap();
        }

        // 创建返回结果的 map
        Map<String, ItemStatus> resultMap = new HashMap<>();

        // 遍历每个 item（寄存器地址）
        for (String item : itemList) {
            try {
                int registerAddress = Integer.parseInt(item); // 转换 item 为寄存器地址

                // 创建 Modbus 请求，根据寄存器类型选择请求类型
                ModbusRequest request = null;

                // 根据寄存器类型构造请求
                if (RegisterTypeEnum.COILS.getCode().equals(registerType)) {
                    request = new ReadCoilsRequest(registerAddress, 1);
                } else if (RegisterTypeEnum.INPUT_REGISTERS.getCode().equals(registerType)) {
                    request = new ReadInputDiscretesRequest(registerAddress, 1);
                } else if (RegisterTypeEnum.HOLDING_REGISTERS.getCode().equals(registerType)) {
                    request = new ReadMultipleRegistersRequest(registerAddress, 1);
                } else if (RegisterTypeEnum.DISCRETE_INPUTS.getCode().equals(registerType)) {
                    request = new ReadInputRegistersRequest(registerAddress, 1);
                } else {
                    log.error("无效的寄存器类型: {}", registerType);
                    continue;  // 跳过此 item
                }

                // 设置 UnitID（设备地址）
                request.setUnitID(Long.valueOf(salveAddr).intValue());

                // 获取连接包装器
                ConnectionWrapper wrapper = getOrCreate(host, port.toString(), registerType, salveAddr);
                TCPMasterConnection connection = wrapper.connection;

                // 创建 Modbus 事务
                ModbusTCPTransaction transaction = new ModbusTCPTransaction(connection);
                transaction.setRequest(request);

                // 执行事务
                transaction.execute();

                // 获取响应数据
                ModbusResponse response = transaction.getResponse();

                // 读取寄存器值
                int registerValue = -1; // 默认无效值
                if (response instanceof ReadMultipleRegistersResponse) {
                    registerValue = ((ReadMultipleRegistersResponse) response).getRegisterValue(0);
                }

                // 创建 ItemStatus 并设置寄存器值
                ItemStatus itemStatus = new ItemStatus();
                itemStatus.setItemId(item);
                itemStatus.setValue(String.valueOf(registerValue));

                // 将 itemStatus 放入 resultMap，使用 item 为 key
                resultMap.put(item, itemStatus);

            } catch (Exception e) {
                // 捕获每个 item 的异常并记录错误
                log.error("读取寄存器 {} 时出错: {}", item, registerType);
                e.printStackTrace();
            }
        }

        // 返回最终的 resultMap
        return resultMap;
    }

    /**
     * 关闭所有 Modbus 连接
     */
    public static void closeAll() {
        cache.values().forEach(wrapper -> {
            try {
                wrapper.connection.close();
            } catch (Exception e) {
                log.warn("Failed to close Modbus connection", e);
            }
        });
        cache.clear();
    }
}
