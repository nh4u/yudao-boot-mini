package cn.bitlinks.ems.framework.common.util.modbus;

import cn.bitlinks.ems.framework.common.util.opcda.ItemStatus;
import cn.bitlinks.ems.framework.common.util.opcda.OpcDaConnectionManager;
import cn.bitlinks.ems.framework.common.util.opcda.OpcDaConnectionMonitor;
import cn.hutool.core.collection.CollUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ModbusUtils {

    public static Map<String, ItemStatus> readOnly(String host, String user, String password, String clsid,
                                                   List<String> itemList) {
        return null;
//        if (CollUtil.isEmpty(itemList)) {
//            return Collections.emptyMap();
//        }
//
//        try {
//            OpcDaConnectionManager.ServerGroupWrapper wrapper =
//                    OpcDaConnectionManager.getOrCreate(host, user, password, clsid);
//
//            // 启动监控
//            OpcDaConnectionMonitor.startMonitoring(host, user, password, clsid);
//
//            return OpcDaConnectionManager.readItems(wrapper, itemList);
//        } catch (Exception e) {
//            log.error("OPC readOnly 失败 [host={}, clsid={}, items={}]", host, clsid, itemList, e);
//            return Collections.emptyMap();
//        }
    }
}
