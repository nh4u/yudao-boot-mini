package cn.bitlinks.ems.framework.common.util.modbus;

import com.ghgande.j2mod.modbus.slave.ModbusSlave;

public class ModbusTcpClientExample {

    public static void main(String[] args) throws Exception {
        ModbusSlave slave = ModbusSlaveFullExample.createModbusTcpServer();

        // 模拟运行 15 分钟，期间从站可被客户端访问
        ModbusSlaveFullExample.close(slave);
    }
}
