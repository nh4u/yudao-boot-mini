package cn.bitlinks.ems.module.power.task;


import cn.bitlinks.ems.framework.common.util.date.LocalDateTimeUtils;
import cn.bitlinks.ems.framework.tenant.core.job.TenantJob;
import cn.bitlinks.ems.module.power.controller.admin.externalapi.vo.ProductYieldMeta;
import cn.bitlinks.ems.module.power.controller.admin.externalapi.vo.ProductionSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.production.ProductionDO;
import cn.bitlinks.ems.module.power.service.externalapi.ExternalApiService;
import cn.bitlinks.ems.module.power.service.production.ProductionService;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.module.power.enums.CommonConstants.PRODUCTION_SYNC_TASK_LOCK_KEY;

/**
 * 产量外部接口 定时任务
 *
 * @author liumingqiang
 */
@Slf4j
@Component
public class ProductionTask {

    @Value("${spring.profiles.active}")
    private String env;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private ExternalApiService externalApiService;

    @Resource
    private ProductionService productionService;

    /**
     * 执行定时任务  同步产量数据到数据表中 （每隔10分钟同步一次）每整小时(0 0 0/1 * * ?)获取一次数据
     */
    @Scheduled(cron = "0 0 0/1 * * ?")
    @TenantJob
    public void execute() {

        String LOCK_KEY = String.format(PRODUCTION_SYNC_TASK_LOCK_KEY, env);

        RLock lock = redissonClient.getLock(LOCK_KEY);
        try {
            if (!lock.tryLock(5000L, TimeUnit.MILLISECONDS)) {
                log.info("产量外部接口Task 已由其他节点执行");
                return;
            }
            try {
                log.info("处理产品产量数据-开始");
                dealProductYield();
                log.info("处理产品产量数据-结束");
            } finally {
                lock.unlock();
            }

        } catch (Exception e) {
            log.error("产量外部接口Task 执行失败", e);
        }
    }

    /**
     * 处理产量数据，并写入缓存当中
     *
     * @return
     */
    private void dealProductYield() {
        Map<String, List<ProductYieldMeta>> productYield = getProductYield();

        if (productYield != null) {
            List<ProductYieldMeta> eight = productYield.get("eight");
            dealProduction(8, eight);
            List<ProductYieldMeta> twelve = productYield.get("twelve");
            dealProduction(12, twelve);
        }
    }

    /**
     * 获取芯片产量数据 8吋、12吋 两种尺寸
     * {
     * "FABOUTTIME":"202101",
     * "PLAN_QTY":40000,
     * "LOT_QTY":16896
     * },
     *
     * @return
     */
    private Map<String, List<ProductYieldMeta>> getProductYield() {
        try {
            String url = externalApiService.getProductYieldUrl();
            log.info("处理产品产量数据-发送请求" + url);
            HttpResponse response = HttpRequest.post(url)
                    .header(Header.CONTENT_TYPE, "application/json;charset=UTF-8")
                    // 超时，毫秒
                    .timeout(20000)
                    .execute();

            // Check status code
            int statusCode = response.getStatus();
            if (statusCode == HttpStatus.HTTP_OK) {

                JSONObject jsonObject = JSON.parseObject(response.body());
                log.info("处理产品产量数据-数据处理");
                // 不转换一下 会报错 com.alibaba.fastjson.JSONObject cannot be cast to cn.bitilinks.doublecarbon.controlplatform.vo.result.monitor.bigscreen.ProductYieldMeta
                List<ProductYieldMeta> eightListTemp = (List<ProductYieldMeta>) JSONPath.eval(jsonObject, "$.8吋");
                List<ProductYieldMeta> eightList = JSON.parseArray(JSON.toJSONString(eightListTemp), ProductYieldMeta.class);

                List<ProductYieldMeta> twelveListTemp = (List<ProductYieldMeta>) JSONPath.eval(jsonObject, "$.12吋");
                List<ProductYieldMeta> twelveList = JSON.parseArray(JSON.toJSONString(twelveListTemp), ProductYieldMeta.class);

                Map<String, List<ProductYieldMeta>> map = new HashMap<>();

                // 处理一下八吋 12吋问题
                map.put("eight", eightList);
                map.put("twelve", twelveList);
                return map;
            } else {
                log.error("获取产量数据失败:" + response.body());
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 处理芯片产量数据
     * 数据示例:
     * {
     * "FABOUTTIME":"202101",
     * "PLAN_QTY":40000,
     * "LOT_QTY":16896
     * },
     *
     * @param size
     * @param productYieldMetaList
     */
    private void dealProduction(Integer size, List<ProductYieldMeta> productYieldMetaList) {

        if (CollUtil.isNotEmpty(productYieldMetaList)) {

            List<ProductYieldMeta> collect = productYieldMetaList
                    .stream()
                    .peek(p -> {
                        String fabOutTime = p.getFABOUTTIME();
                        int year = Integer.parseInt(fabOutTime.substring(0, 4));
                        int month = Integer.parseInt(fabOutTime.substring(4, 6));
                        LocalDateTime startTime = LocalDateTimeUtils.startOfMonth(year, month);
                        p.setTime(startTime);
                    })
                    .sorted(Comparator.comparing(ProductYieldMeta::getTime).reversed())
                    .collect(Collectors.toList());

            if (CollUtil.isNotEmpty(collect)) {
                saveProductionData(size, collect.get(0));
            }
        } else {
            log.info("尺寸：【{}吋】，产量数据为空", size);
        }
    }

    private void saveProductionData(Integer size, ProductYieldMeta productYieldMeta) {

        ProductionSaveReqVO vo = new ProductionSaveReqVO();
        vo.setOriginTime(productYieldMeta.getFABOUTTIME());
        vo.setPlan(productYieldMeta.getPLAN_QTY());
        vo.setLot(productYieldMeta.getLOT_QTY());
        vo.setSize(size);

        // 2. 去掉分和秒：保留 年、月、日、时，分和秒设置为 0
        LocalDateTime time = LocalDateTime.now()
                .withMinute(0)   // 分钟设置为 0
                .withSecond(0)   // 秒设置为 0
                .withNano(0);    // 纳秒也清零（可选，通常建议加上）

        // 当前取的数据是上一小时的值
        vo.setTime(time.minusHours(1));

        // 计算差值
        ProductionDO last = productionService.getLastProduction(size);
        if (Objects.nonNull(last)) {
            BigDecimal value = vo.getLot().subtract(last.getLot());
            vo.setValue(value);
        }

        log.info("处理产品产量数据-保存" + vo);
        productionService.createProduction(vo);
    }
}