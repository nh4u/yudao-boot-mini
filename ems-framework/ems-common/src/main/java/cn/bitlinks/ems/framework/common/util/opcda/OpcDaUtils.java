package cn.bitlinks.ems.framework.common.util.opcda;

import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.openscada.opc.lib.da.Group;
import org.openscada.opc.lib.da.Server;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * opc-da协议 获取数据
 */
@Slf4j
public class OpcDaUtils {

    public static Map<String, ItemStatus> batchGetValue(String host, String user, String password, String clsid,
                                                        List<String> itemList) {
        if (CollUtil.isEmpty(itemList)) {
            return Collections.emptyMap();
        }
        String[] items = itemList.toArray(new String[0]);
        try {
            Server server = OpcDaConnectionManager.getServer(host, user, password, clsid);
            OpcDaConnectionMonitor.startMonitoring(host, user, password, clsid);
            Group group = server.addGroup("DynamicGroup");
            group.setActive(true);
            group.addItems(items);
            return OpcUtil.readValues(group, itemList);
        } catch (Exception e) {
            // 记录日志或处理异常
            log.error("Failed to read values from OPC server", e);
            return null;
        }
    }
}
