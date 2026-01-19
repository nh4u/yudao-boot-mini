//package cn.bitlinks.ems.module.acquisition.task;
//
//import cn.bitlinks.ems.framework.common.util.opcda.ItemStatus;
//import cn.bitlinks.ems.framework.common.util.opcda.OpcDaUtils;
//import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
//import cn.bitlinks.ems.module.acquisition.dal.mysql.collectrawdata.CollectRawDataMapper;
//import cn.bitlinks.ems.module.acquisition.task.entity.AcConfig;
//import com.baomidou.dynamic.datasource.annotation.DS;
//import lombok.extern.slf4j.Slf4j;
//import org.redisson.api.RLock;
//import org.redisson.api.RedissonClient;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//import javax.annotation.Resource;
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.TimeUnit;
//
//import static cn.bitlinks.ems.module.acquisition.enums.CommonConstants.AIR_CONDITIONER_TASK_LOCK_KEY;
//
///**
// * 空调工况采集任务
// */
//@Slf4j
//@Component
//@DS("starrocks")
//public class AirConditionerTask {
//    @Value("${spring.profiles.active}")
//    private String env;
//    @Resource
//    private RedissonClient redissonClient;
//
//    /**
//     * 采集任务:每分钟0秒一次30秒执行一次。
//     */
//    @Scheduled(cron = "0,30 * * * * ?") // 每分钟0秒一次30秒执行一次。
//    public void executeAcq() {
//        String LOCK_KEY = String.format(AIR_CONDITIONER_TASK_LOCK_KEY, env);
//
//        // 对齐任务时间
//        LocalDateTime jobTime = LocalDateTime.now()
//                .withNano(0)
//                .withSecond((LocalDateTime.now().getSecond() / 30) * 30);
//        log.info("开始执行[{}]", jobTime);
//        RLock lock = redissonClient.getLock(LOCK_KEY);
//
//        try {
//            if (!lock.tryLock(5000L, TimeUnit.SECONDS)) {
//                log.info("空调工况采集Task 已由其他节点执行");
//            }
//            try {
//                // 异步调用采集任务，
//                airConditioner(jobTime);
//            } finally {
//                lock.unlock();
//            }
//        } catch (Exception e) {
//            log.error("空调工况采集采集Task 执行失败", e);
//        }
//
//    }
//
//    private void airConditioner(LocalDateTime jobTime) {
//        // 1. 从配置表/缓存中获取所有空调的采集配置
//        List<AcConfig> configList = loadAcConfigs();
//        for (AcConfig config : configList) {
//            // 2. 异步执行每一个连接配置的采集，避免前面的空调连接超时影响后面的空调
//            CompletableFuture.runAsync(() -> {
//                try {
//                    // 3. 执行您已有的采集方法
//                    Map<String, ItemStatus> result = OpcDaUtils.readOnly(
//                            config.getIp(),
//                            config.getUsername(),
//                            config.getPassword(),
//                            config.getClsid(),
//                            config.getIoAddresses()
//                    );
//
//                    // 4. 处理采集结果并调用入库逻辑
//                    // processAndSave(config.getId(), result);
//
//                } catch (Exception e) {
//                    log.error("空调采集失败, 服务配置【ip:{},user:{},password:{},clsid:{}】,异常：{}", config.getIp(), config.getUsername(), config.getPassword(), config.getClsid(),e.getMessage());
//                }
//            }, executor);
//        }
//    }
//
//    private void processAndSave(String acId, Map<String, ItemStatus> data) {
//        // 在这里将 Map 转换为您入库需要的 Entity 对象
//        // 然后调用 StarRocks 的入库接口（如 Stream Load 或 Connector）
//        System.out.println("空调 " + acId + " 采集完成，准备入库 " + data.size() + " 个点位数据");
//    }
//
//    private List<AcConfig> loadAcConfigs() {
//        // 实际开发中，这里应该从数据库读取
//        // 从缓存中读取配置信息
//        return new ArrayList<>();
//    }
//
//}
