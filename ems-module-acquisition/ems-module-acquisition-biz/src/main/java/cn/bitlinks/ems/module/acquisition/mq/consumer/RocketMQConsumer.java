package cn.bitlinks.ems.module.acquisition.mq.consumer;

import cn.bitlinks.ems.framework.common.core.ParameterKey;
import cn.bitlinks.ems.framework.common.core.StandingbookAcquisitionDetailDTO;
import cn.bitlinks.ems.framework.common.util.calc.AcquisitionFormulaUtils;
import cn.bitlinks.ems.framework.common.util.opcda.ItemStatus;
import cn.bitlinks.ems.framework.common.util.opcda.OpcDaUtils;
import cn.bitlinks.ems.module.acquisition.api.job.dto.ServiceSettingsDTO;
import cn.bitlinks.ems.module.acquisition.dal.dataobject.collectrawdata.CollectRawDataDO;
import cn.bitlinks.ems.module.acquisition.mq.message.AcquisitionMessage;
import cn.bitlinks.ems.module.acquisition.service.collectrawdata.CollectRawDataService;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.framework.common.enums.CommonConstants.SPRING_PROFILES_ACTIVE_PROD;

@Slf4j
public abstract class RocketMQConsumer implements RocketMQListener<AcquisitionMessage> {
    @Value("${spring.profiles.active}")
    private String env;
    @Resource
    private CollectRawDataService collectRawDataService;
    /**
     * mock数据初始值 上限
     */
    final BigDecimal MOCK_INIT_MAX = new BigDecimal("600");
    /**
     * mock数据初始值 下限
     */
    final BigDecimal MOCK_INIT_MIN = new BigDecimal("100");
    /**
     * mock数据增量值 上限
     */
    final BigDecimal MOCK_INCREMENT_MAX = new BigDecimal("5");
    /**
     * mock数据增量值 下限
     */
    final BigDecimal MOCK_INCREMENT_MIN = new BigDecimal("0");

    @Override
    public void onMessage(AcquisitionMessage acquisitionMessage) {

        log.info("收到消息：{}", JSONUtil.toJsonStr(acquisitionMessage));

        List<StandingbookAcquisitionDetailDTO> deviceParamDetails = acquisitionMessage.getDetails();

        if (CollUtil.isEmpty(deviceParamDetails)) {
            log.info("设备[{}] 无可采集参数!", acquisitionMessage.getStandingbookId());
            return;
        }
        // 过滤出 status = true && (dataSite 不为空 或 公式不为空) 的参数
        List<StandingbookAcquisitionDetailDTO> paramDetails =
                deviceParamDetails.stream().filter(detail -> Boolean.TRUE.equals(detail.getStatus()) && (StringUtils.isNotEmpty(detail.getDataSite()) || StringUtils.isNotEmpty(detail.getActualFormula()))).collect(Collectors.toList());
        if (CollUtil.isEmpty(paramDetails)) {
            log.info("设备[{}] 无可采集参数, 没有需要采集的参数!", acquisitionMessage.getStandingbookId());
            return;
        }
        // 过滤出有io地址的需要采集的参数值
        // 筛选 dataSite 不为空的记录
        List<String> dataSites = paramDetails.stream()
                .filter(detail -> StringUtils.isNotEmpty(detail.getDataSite()))
                .map(StandingbookAcquisitionDetailDTO::getDataSite)
                .collect(Collectors.toList());

        if (CollUtil.isEmpty(dataSites)) {
            log.info("设备[{}] 无可采集参数, 没有配置io的参数!", acquisitionMessage.getStandingbookId());
            return;
        }

        // 采集有io的参数的真实数据
        Map<String, ItemStatus> itemStatusMap;
        if (env.equals(SPRING_PROFILES_ACTIVE_PROD)) {
            ServiceSettingsDTO serviceSettingsDTO = acquisitionMessage.getServiceSettingsDTO();
            // 采集所有参数
            itemStatusMap = OpcDaUtils.batchGetValue(serviceSettingsDTO.getIpAddress(),
                    serviceSettingsDTO.getUser(),
                    serviceSettingsDTO.getPassword(), serviceSettingsDTO.getClsid(), dataSites);
        } else {
            itemStatusMap = mockData(dataSites, acquisitionMessage.getStandingbookId());
        }
        if(CollUtil.isEmpty(itemStatusMap)){
            log.info("设备[{}] 无可采集参数, 配置的io采集不到数据!", acquisitionMessage.getStandingbookId());
            return;
        }
        List<CollectRawDataDO> collectRawDataDOList = new ArrayList<>();
        // 将采集到的数据插入实时数据库
        Map<ParameterKey,
                StandingbookAcquisitionDetailDTO> paramMap = new HashMap<>();
        for (StandingbookAcquisitionDetailDTO detail : paramDetails) {
            ParameterKey key = new ParameterKey(detail.getCode(), detail.getEnergyFlag());
            paramMap.put(key, detail);
        }

        paramMap.forEach((key, detail) -> {
            // 计算公式的值
            String calcValue = AcquisitionFormulaUtils.calcSingleParamValue(detail, paramMap, itemStatusMap);
            if (StringUtils.isEmpty(calcValue)) {
                return;
            }
            // 计算出值, 将数据带入实时数据表中.
            CollectRawDataDO collectRawDataDO = new CollectRawDataDO();
            collectRawDataDO.setDataSite(detail.getDataSite());
            collectRawDataDO.setStandingbookId(acquisitionMessage.getStandingbookId());
            collectRawDataDO.setSyncTime(acquisitionMessage.getJobTime());
            collectRawDataDO.setParamCode(detail.getCode());
            collectRawDataDO.setEnergyFlag(detail.getEnergyFlag());
            collectRawDataDO.setCalcValue(calcValue);
            ItemStatus itemStatus = itemStatusMap.get(detail.getDataSite());
            if (Objects.nonNull(itemStatus)) {
                collectRawDataDO.setRawValue(itemStatus.getValue());
                collectRawDataDO.setCollectTime(itemStatus.getTime());
            }
            collectRawDataDOList.add(collectRawDataDO);
        });

        if (CollUtil.isEmpty(collectRawDataDOList)) {
            return;
        }
        // 执行插入操作
        collectRawDataService.insertBatch(acquisitionMessage.getStandingbookId(), collectRawDataDOList);
        // 实时数据 触发告警规则


        // 实时数据 拆分/聚合到 分钟聚合数据表 todo


    }

    /**
     * 模拟采集的数据
     *
     * @param dataSites 数据点位集合
     */
    private Map<String, ItemStatus> mockData(List<String> dataSites, Long standingbookId) {
        Map<String, ItemStatus> resultMap = new HashMap<>();
        // 查询表中该io最新的采集数据的值
        List<CollectRawDataDO> exist =
                collectRawDataService.selectLatestByStandingbookId(standingbookId);
        Map<String, String> existValueMap = new HashMap<>();
        if (CollUtil.isNotEmpty(exist)) {
            exist.forEach(collectRawDataDO -> existValueMap.put(collectRawDataDO.getDataSite(), collectRawDataDO.getRawValue()));
        }
        // mock 采集时间
        LocalDateTime collectTime = LocalDateTime.now();
        dataSites.forEach(dataSite -> {
            ItemStatus itemStatus = new ItemStatus();
            itemStatus.setItemId(dataSite);
            //如果不存在最新数据, 随机生成 100-600之间的数值
            if (StringUtils.isNotEmpty(existValueMap.get(dataSite))) {
                itemStatus.setValue(getRandomValue(MOCK_INIT_MAX, MOCK_INIT_MIN));
            } else {
                // 存在最新数据, 在最新数据的基础上进行随机0-5的数据的增加
                String oldValue = existValueMap.get(dataSite);
                String increment = getRandomValue(MOCK_INCREMENT_MAX, MOCK_INCREMENT_MIN);
                BigDecimal newValue = new BigDecimal(oldValue).add(new BigDecimal(increment));
                itemStatus.setValue(newValue.toString());
            }
            itemStatus.setTime(collectTime);
            resultMap.put(dataSite, itemStatus);
        });
        return resultMap;

    }

    /**
     * 生成随机数据
     *
     * @param max 范围
     * @param min 范围
     * @return 随机数据
     */
    private String getRandomValue(BigDecimal max, BigDecimal min) {
        // 使用 Random 生成随机数
        Random random = new Random();
        // 生成 [0, 1) 的随机值，并缩放到 [min, max]
        BigDecimal range = max.subtract(min);
        BigDecimal randomValue =
                min.add(range.multiply(BigDecimal.valueOf(random.nextDouble()))).setScale(6,
                        RoundingMode.UP);
        return randomValue.toString();
    }

}
