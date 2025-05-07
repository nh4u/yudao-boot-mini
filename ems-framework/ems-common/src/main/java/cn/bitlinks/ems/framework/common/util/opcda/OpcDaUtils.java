package cn.bitlinks.ems.framework.common.util.opcda;

import org.openscada.opc.lib.common.ConnectionInformation;
import org.openscada.opc.lib.da.Server;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * opc-da协议
 */
public class OpcDaUtils {


    public static boolean testLink(String host, String user, String password, String clsid) {
        // 连接信息
        final ConnectionInformation ci = new ConnectionInformation();
        ci.setHost(host);         // 电脑IP
        ci.setDomain("");                  // 域，为空就行
        ci.setUser(user);             // 电脑上自己建好的用户名
        ci.setPassword(password);          // 用户名的密码

        // 创建 OPC 服务器对象
        // 使用 KEPServer 的配置
        ci.setClsid(clsid); // KEPServer 的注册表ID

        // 创建一个调度执行器
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

        // 创建 Server 对象
        final Server server = new Server(ci, executor);

        try {
            // 连接到 OPC 服务器
            server.connect();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            //  断开连接
            try {
                server.disconnect();
            } finally {
                executor.shutdown(); //关闭executor
            }
        }
    }

}
