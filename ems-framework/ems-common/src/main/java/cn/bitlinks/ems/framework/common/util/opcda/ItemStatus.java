package cn.bitlinks.ems.framework.common.util.opcda;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备采集状态
 */
@Data
public class ItemStatus {
    /**
     * 设备标识
     */
    String itemId;
    /**
     * 设备值
     */
    String value;
    /**
     * 采集时间
     */
    LocalDateTime time;
}
