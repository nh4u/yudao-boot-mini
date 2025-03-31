package cn.bitlinks.ems.module.power.service.starrocks;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 *  Service 接口
 *
 * @author bitlinks
 */
public interface StarrocksService {
    /**
     * 获取Starrocks示例数据
     * @return
     */
    List<Map<String, Objects>> queryData();

    void addData(String date);

    void deleteData(String date);
}