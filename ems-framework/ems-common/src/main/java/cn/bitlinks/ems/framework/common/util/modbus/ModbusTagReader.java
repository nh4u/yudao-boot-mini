package cn.bitlinks.ems.framework.common.util.modbus;

import com.ghgande.j2mod.modbus.Modbus;
import com.ghgande.j2mod.modbus.io.ModbusTCPTransaction;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersResponse;
import com.ghgande.j2mod.modbus.net.TCPMasterConnection;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ModbusTagReader {

    // 连接配置
    private static final String HOST = "127.0.0.1";  // ← 请改为你的设备 IP
    private static final int PORT = Modbus.DEFAULT_PORT; // 通常是 502
    private static final int UNIT_ID = 1;                // 从站编号

    // 读取时间配置
    //  private static final int INTERVAL_MINUTES = 1;       // 每次间隔
    private static final int INTERVAL_MINUTES = 5;       // 每次间隔
    private static final int TOTAL_DURATION_MINUTES = 180;// 总次数180

    private static final String EXCEL_FILE_PATH = "D:\\施耐德.xlsx";

    // 标签与寄存器地址映射
    private static final LinkedHashMap<String, Integer> TAG_ADDRESS_MAP = new LinkedHashMap<>();

    static {
        TAG_ADDRESS_MAP.put("LCR-电机电流", 3204);
        TAG_ADDRESS_MAP.put("NPR-电机额定功率", 9613);
        TAG_ADDRESS_MAP.put("UOP-电机电压-V", 3208);
        TAG_ADDRESS_MAP.put("OPR-电机功率-%", 3211);
        TAG_ADDRESS_MAP.put("OC0-电机消耗的电能-Wh", 10606);
        TAG_ADDRESS_MAP.put("OC1-电机消耗的电能-KWh", 10607);
        TAG_ADDRESS_MAP.put("OC2-电机消耗的电能-MWh", 10608);
        TAG_ADDRESS_MAP.put("OC3-电机消耗的电能-GWh", 10609);
        TAG_ADDRESS_MAP.put("OC4-电机消耗的电能-TWh", 10610);
        TAG_ADDRESS_MAP.put("OP0-电机产生的电能-WH", 10611);
        TAG_ADDRESS_MAP.put("OP1-电机产生的电能-KWh", 10612);
        TAG_ADDRESS_MAP.put("OP2-电机产生的电能-MWh", 10613);
        TAG_ADDRESS_MAP.put("OP3-电机产生的电能-GWh", 10614);
        TAG_ADDRESS_MAP.put("OP4-电机产生的电能-TWh", 10615);
        TAG_ADDRESS_MAP.put("OE0-实际能耗-WH", 10616);
        TAG_ADDRESS_MAP.put("OE1-实际能耗-KWh", 10617);
        TAG_ADDRESS_MAP.put("OE2-实际能耗-MWh", 10618);
        TAG_ADDRESS_MAP.put("OE3-实际能耗-GWh", 10619);
        TAG_ADDRESS_MAP.put("OE4-实际能耗-TWh", 10620);
    }

    // Excel 操作变量
    private static final Workbook workbook = new XSSFWorkbook();
    private static final Sheet sheet = workbook.createSheet("ModbusData");
    private static int rowIndex = 0;

    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public static void main(String[] args) throws Exception {
        // ModbusSlave modbusTcpServer = ModbusSlaveFullExample.createModbusTcpServer();

        System.out.println("开始读取 Modbus TCP 数据，每分钟执行一次，共 " + TOTAL_DURATION_MINUTES + " 次...");

        final int[] runCount = {0};

        scheduler.scheduleAtFixedRate(() -> {
            try {
                List<Integer> values = readAllTags();
                System.out.println("第 " + (runCount[0] + 1) + " 次读取成功: " + values);
            } catch (Exception e) {
                System.err.println("读取失败: " + e.getMessage());
            }

            runCount[0]++;
            if (runCount[0] >= TOTAL_DURATION_MINUTES) {
                scheduler.shutdown();
                System.out.println("任务完成");
            }

        }, 0, INTERVAL_MINUTES, TimeUnit.SECONDS);

        //ModbusSlaveFullExample.close(modbusTcpServer);
    }

    // 读取所有地址
    private static List<Integer> readAllTags() throws Exception {
        List<Integer> result = new ArrayList<>();
        InetAddress addr = InetAddress.getByName(HOST);

        try {
            TCPMasterConnection connection = new TCPMasterConnection(addr);

            connection.setPort(PORT);
            connection.connect();

            for (int registerAddress : TAG_ADDRESS_MAP.values()) {
                //不同的寄存器类型需要不同的类，比如离散寄存器使用ReadInputDiscretesRequest
                //当前读取的是保存寄存器的数据
                ReadMultipleRegistersRequest req = new ReadMultipleRegistersRequest(registerAddress, 1);
                req.setUnitID(UNIT_ID);

                ModbusTCPTransaction trans = new ModbusTCPTransaction(connection);
                trans.setRequest(req);
                trans.execute();

                ReadMultipleRegistersResponse res = (ReadMultipleRegistersResponse) trans.getResponse();
                result.add(res.getRegisterValue(0));
            }
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("获取数据失败");
        }

        return result;
    }


}


