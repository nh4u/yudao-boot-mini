package cn.bitlinks.ems.framework.common.util.modbus;

import cn.bitlinks.ems.framework.common.enums.RegisterTypeEnum;
import cn.bitlinks.ems.framework.common.util.opcda.ItemStatus;
import com.ghgande.j2mod.modbus.io.ModbusTCPTransaction;
import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.net.TCPMasterConnection;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.bitlinks.ems.framework.common.util.modbus.ModbusConnectionManager.createModbusRequest;
import static cn.bitlinks.ems.framework.common.util.modbus.ModbusConnectionManager.parseResponse;


@Slf4j
public class ModbusUtils {

    public static Map<String, ItemStatus> readOnly(String host, Integer port, String registerType, String salveAddr,
                                                   List<String> itemList) {
        if (itemList == null || itemList.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            // 获取或创建 Modbus 连接
            ModbusConnectionManager.ConnectionWrapper wrapper =
                    ModbusConnectionManager.getOrCreate(host, port.toString(), registerType, salveAddr);

            // 启动连接监控（可选）
            ModbusConnectionMonitor monitor = new ModbusConnectionMonitor(host, port, registerType, salveAddr);
            monitor.startMonitoring();

            Map<String, ItemStatus> resultMap = new HashMap<>();

            // 批量读取寄存器
            for (String item : itemList) {
                try {
                    int registerAddress = Integer.parseInt(item);
                    ModbusRequest request = createModbusRequest(
                            RegisterTypeEnum.codeOf(registerType), registerAddress);
                    if (request == null) {
                        log.error("无效的寄存器类型: {}", registerType);
                        continue;
                    }
                    request.setUnitID(Integer.parseInt(salveAddr));

                    TCPMasterConnection connection = wrapper.connection;
                    ModbusTCPTransaction transaction = new ModbusTCPTransaction(connection);
                    transaction.setRequest(request);
                    transaction.execute();

                    ModbusResponse response = transaction.getResponse();
                    ItemStatus itemStatus = parseResponse(response, item);
                    resultMap.put(item, itemStatus);
                } catch (Exception e) {
                    log.error("读取寄存器 {} 出错", item, e);
                }
            }

            return resultMap;

        } catch (Exception e) {
            log.error("Modbus readOnly 失败 [host={}, port={}, items={}]", host, port, itemList, e);
            return Collections.emptyMap();
        }
    }

}
