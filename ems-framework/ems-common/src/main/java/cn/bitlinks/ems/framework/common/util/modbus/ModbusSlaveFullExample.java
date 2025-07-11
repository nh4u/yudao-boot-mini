package cn.bitlinks.ems.framework.common.util.modbus;

import com.ghgande.j2mod.modbus.procimg.SimpleProcessImage;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;
import com.ghgande.j2mod.modbus.slave.ModbusSlave;
import com.ghgande.j2mod.modbus.slave.ModbusSlaveFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ModbusSlaveFullExample {

    // 你给的寄存器地址列表
    private static final List<Integer> REGISTER_ADDRESSES = Arrays.asList(
            3204, 3208, 3211,
            10606, 10607, 10608, 10609, 10610,
            10611, 10612, 10613, 10614, 10615,
            10616, 10617, 10618, 10619, 10620
    );

    private static final int PORT = 502;
    private static final int UNIT_ID = 1;
    private static final boolean USE_RTU_OVER_TCP = false; // 标准Modbus TCP
    private static final int THREAD_POOL_SIZE = 3;
    private static final int RUN_MINUTES = 15;

    // 定时每秒更新一次指定寄存器的随机值
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();


    public static ModbusSlave createModbusTcpServer()throws Exception{
        // 创建Modbus从站实例
        ModbusSlave slave = ModbusSlaveFactory.createTCPSlave(PORT, THREAD_POOL_SIZE);

        // 计算最大寄存器地址，为连续分配内存用
        int maxAddress = Collections.max(REGISTER_ADDRESSES);

        // 新建ProcessImage
        SimpleProcessImage spi = new SimpleProcessImage();

        // 按连续地址预置保持寄存器，默认值0
        SimpleRegister[] registers = new SimpleRegister[maxAddress + 1];
        for (int i = 0; i <= maxAddress; i++) {
            registers[i] = new SimpleRegister(0);
            spi.addRegister(registers[i]);
        }

        // 将ProcessImage绑定到unitId
        slave.addProcessImage(UNIT_ID, spi);

        // 启动Modbus从站
        slave.open();
        System.out.println("Modbus TCP从站启动，端口=" + PORT + "，UnitId=" + UNIT_ID);

        scheduler.scheduleAtFixedRate(() -> {
            Random random = new Random();
            for (int addr : REGISTER_ADDRESSES) {
                int val = random.nextInt(10);
                registers[addr].setValue(val);
            }
            System.out.println("随机寄存器数据更新完毕");
        }, 0, 30, TimeUnit.SECONDS);

       return slave;
    }

    public static void close (ModbusSlave slave) throws InterruptedException {
        Thread.sleep(15 * 60 * 1000);
        scheduler.shutdownNow();
        slave.close();
        System.out.println("Modbus TCP从站停止");
    }



}
