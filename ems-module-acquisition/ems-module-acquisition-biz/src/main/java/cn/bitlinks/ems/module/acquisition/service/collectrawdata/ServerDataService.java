package cn.bitlinks.ems.module.acquisition.service.collectrawdata;

import cn.bitlinks.ems.framework.common.util.json.JsonUtils;
import cn.bitlinks.ems.framework.common.util.opcda.ItemStatus;
import cn.bitlinks.ems.framework.common.util.opcda.OpcDaUtils;
import cn.bitlinks.ems.framework.common.util.string.StrUtils;
import cn.bitlinks.ems.module.power.dto.ServerParamsCacheDTO;
import cn.hutool.core.text.CharSequenceUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.module.acquisition.enums.CommonConstants.COLLECTOR_AGG_REALTIME_CACHE_KEY;
import static cn.bitlinks.ems.module.power.enums.RedisKeyConstants.STANDING_BOOK_SERVER_IO_CONFIG;

@Slf4j
@Service
public class ServerDataService {

    // 注入配置好的线程池
    @Autowired
    @Qualifier("collectorAggExecutor")
    private ThreadPoolTaskExecutor collectorAggExecutor;

    @Resource
    private RedisTemplate<String, byte[]> byteArrayRedisTemplate;
    @Resource
    private RedisTemplate<String, String> redisTemplate;

    public void processServerData() {
        Map<String, List<String>> serverIoMapping = getServerIoMapping();
        if (serverIoMapping == null || serverIoMapping.isEmpty()) {
            return;
        }

        // 遍历所有服务器信息，并发处理
        serverIoMapping.forEach((serverKey, ioAddresses) -> {
            // 提交任务到线程池异步执行
            CompletableFuture.runAsync(() -> {
                try {
                    // 解析serverKey获取连接信息
                    String[] serverInfo = serverKey.split("\\|");

                    //String serverType = serverInfo[0];
                    String host = serverInfo[1];
                    String user = serverInfo[2];
                    String password = serverInfo[3];
                    String clsid = serverInfo[4];
                    // 执行OPC数据采集
                    Map<String, ItemStatus> result = OpcDaUtils.readOnly(
                            host, user, password, clsid, ioAddresses
                    );

                    // 处理采集结果（存储到Redis）
                    saveResultToRedis(serverKey, result);

                } catch (Exception e) {
                    // 单个任务异常不影响其他任务
                    log.error("处理服务器[{}]数据失败", serverKey, e);
                }
            }, collectorAggExecutor); // 指定使用注入的线程池
        });
    }

    public void saveResultToRedis(String serverKey, Map<String, ItemStatus> result) {
        if (result == null || result.isEmpty()) {
            return;
        }
        String timestampStr = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());

        String redisKey = String.format(COLLECTOR_AGG_REALTIME_CACHE_KEY, serverKey, timestampStr);

        // 使用 pipeline 批量写入 Hash
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            byte[] keyBytes = redisTemplate.getStringSerializer().serialize(redisKey);

            for (Map.Entry<String, ItemStatus> entry : result.entrySet()) {
                byte[] fieldBytes = redisTemplate.getStringSerializer().serialize(entry.getKey());
                // 这里假设ItemStatus重写了toString或者你可以改成JSON序列化
                byte[] valueBytes = redisTemplate.getStringSerializer().serialize(JsonUtils.toJsonString(entry.getValue()));

                connection.hSet(keyBytes, fieldBytes, valueBytes);
            }

            // 你可以根据需求设置整个Hash的过期时间（单位秒） 5分钟的redis热数据存储时间
            connection.expire(keyBytes, 300);

            return null;
        });
    }


    /**
     * 从redis中获取数采服务器与io映射关系
     *
     * @return
     */
    private Map<String, List<String>> getServerIoMapping() {
        byte[] compressed = byteArrayRedisTemplate.opsForValue().get(STANDING_BOOK_SERVER_IO_CONFIG);

        // 先判断缓存是否存在，避免空指针
        if (compressed == null) {
            log.info("缓存不存在，返回空映射");
            return new HashMap<>(); // 或返回null，根据业务需求处理
        }

        try {
            String cacheRes = StrUtils.decompressGzip(compressed);
            if (CharSequenceUtil.isNotEmpty(cacheRes)) {
                // 用 Jackson 处理泛型转换
                List<ServerParamsCacheDTO> serverDataSiteList = JsonUtils.parseArray(cacheRes, ServerParamsCacheDTO.class);
                return serverDataSiteList.stream()
                        .collect(Collectors.groupingBy(
                                ServerParamsCacheDTO::getServerKey,
                                Collectors.mapping(ServerParamsCacheDTO::getDataSite, Collectors.toList())
                        ));
//                ObjectMapper objectMapper = new ObjectMapper();
//                return objectMapper.readValue(cacheRes,
//                        new TypeReference<M>() {
//                        });
            }
        } catch (Exception e) {
            log.error("解析缓存的服务器IO映射关系失败", e);
        }

        return new HashMap<>(); // 解析失败时返回空映射
    }

}
    