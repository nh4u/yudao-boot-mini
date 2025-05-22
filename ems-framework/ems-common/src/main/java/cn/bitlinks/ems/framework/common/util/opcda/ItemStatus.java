package cn.bitlinks.ems.framework.common.util.opcda;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 设备采集状态
 */
@Data
public class ItemStatus implements Serializable {
    private static final long serialVersionUID = 1L; // 推荐指定序列化版本
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
