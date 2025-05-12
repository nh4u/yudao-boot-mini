package cn.bitlinks.ems.framework.common.util.opcda;

import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.openscada.opc.lib.common.ConnectionInformation;
import org.openscada.opc.lib.da.Group;
import org.openscada.opc.lib.da.Server;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * opc-da协议
 */
@Slf4j
public class OpcDaUtils {


    /**
     * 一次性获取批量数据
     *
     * @param host     电脑IP
     * @param user     电脑上自己建好的用户名
     * @param password 用户名的密码
     * @param clsid    KEPServer 的注册表ID
     * @param itemList 数据项标识列表
     * @return 数据值
     */
    public static Map<String, ItemStatus> batchGetValue(String host, String user, String password, String clsid,
                                                        List<String> itemList) {
        if (CollUtil.isEmpty(itemList)) {
            return Collections.emptyMap();
        }
        // 连接信息
        String[] items = itemList.toArray(new String[0]);
        // 连接信息
        final ConnectionInformation ci = new ConnectionInformation();
        ci.setHost(host);         // 电脑IP
        ci.setDomain("");                  // 域，为空就行
        ci.setUser(user);             // 电脑上自己建好的用户名
        ci.setPassword(password);          // 用户名的密码

        // 创建 OPC 服务器对象
        // 使用 KEPServer 的配置
        ci.setClsid(clsid); // KEPServer 的注册表ID

        // 启动服务
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        final Server server = new Server(ci, executor);

        try {
            // 1.连接到 OPC 服务器
            server.connect();
            // 2. 创建OPC Group用于批量读取
            Group group = server.addGroup("DynamicGroup");
            group.setActive(true);  // 激活Group

            // 3.批量读取
            group.addItems(items);
            return OpcUtil.readValues(group, itemList);
        } catch (Exception e) {
            return null;
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
