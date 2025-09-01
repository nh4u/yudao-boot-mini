package cn.bitlinks.ems.module.power.service.standingbook;

import cn.bitlinks.ems.framework.common.util.json.JsonUtils;
import cn.bitlinks.ems.framework.common.util.string.StrUtils;
import cn.bitlinks.ems.framework.dict.core.DictFrameworkUtils;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.MeterRelationExcelDTO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookDTO;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.module.power.enums.DictTypeConstants.STAGE;
import static cn.bitlinks.ems.module.power.enums.RedisKeyConstants.STANDING_BOOK_EXCEL_RELATION;

/**
 * 计量器具关联关系业务服务（实际业务需对接数据库/缓存）
 */
@Slf4j
@Service("meterRelationService")
public class MeterRelationService {
    @Resource
    @Lazy
    private StandingbookService standingbookService;
    @Resource
    private RedisTemplate<String, byte[]> byteArrayRedisTemplate;

    /**
     * 校验计量器具编号是否在系统中存在
     *
     * @param meterCode 计量器具编号
     * @return true=存在，false=不存在
     */
    public boolean checkMeterCodeExists(String meterCode) {
        Set<String> codeList = standingbookService.getStandingbookCodeMeasurementSet();
        if (CollUtil.isEmpty(codeList)) {
            return false;
        }
        return codeList.contains(meterCode);
    }

    /**
     * 校验多个下级计量器具编号是否存在（返回不存在的编号）
     *
     * @param subMeterCodes 下级计量器具编号数组
     * @return 不存在的编号集合（空=全部存在）
     */
    public Set<String> checkSubMeterCodesExists(String[] subMeterCodes) {
        Set<String> codeList = standingbookService.getStandingbookCodeMeasurementSet();
        if (CollUtil.isEmpty(codeList)) {
            return new HashSet<>(Arrays.asList(subMeterCodes));
        }
        return Arrays.asList(subMeterCodes).stream()
                .filter(code -> !codeList.contains(code))
                .collect(Collectors.toSet());
    }

    /**
     * 校验关联设备是否存在
     */
    public boolean checkDeviceExists(String device) {
        Set<String> codeList = standingbookService.getStandingbookCodeDeviceSet();
        if (CollUtil.isEmpty(codeList)) {
            return false;
        }
        return codeList.contains(device);
    }

    /**
     * 校验环节是否存在
     */
    public boolean checkLinkExists(String link) {
        List<String> systemLinks = DictFrameworkUtils.getDictDataLabelList(
                STAGE);
        return systemLinks.contains(link);
    }

    /**
     * 批量暂存合法数据（避免内存溢出，解析完成后统一入库）
     */
    public void batchSaveTemp(List<MeterRelationExcelDTO> validDataList) {
        // 实际逻辑：存入Redis缓存
        byteArrayRedisTemplate.opsForValue().set(STANDING_BOOK_EXCEL_RELATION, StrUtils.compressGzip(JsonUtils.toJsonString(validDataList)));
        log.info("暂存合法数据{}条", validDataList.size());
    }

    /**
     * 最终入库（Excel解析完成且无错误时调用）
     */
    public int batchSaveToDb() {
        // 实际逻辑：从临时表/缓存读取数据，批量插入正式表 todo
        byte[] compressed = byteArrayRedisTemplate.opsForValue().get(STANDING_BOOK_EXCEL_RELATION);

        // 先判断缓存是否存在，避免空指针
        if (compressed == null) {
            return 0;
        }
        try {
            String cacheRes = StrUtils.decompressGzip(compressed);
            if (CharSequenceUtil.isNotEmpty(cacheRes)) {
                // 用 Jackson 处理泛型转换
                List<MeterRelationExcelDTO> serverStandingbookList = JsonUtils.parseArray(cacheRes, MeterRelationExcelDTO.class);

                // 真正入库逻辑
                List<StandingbookDTO> allStandingbookDTOList = standingbookService.getStandingbookDTOList();
                Map<String, Long> sbCodeIdMapping = allStandingbookDTOList.stream()
                        .collect(Collectors.toMap(
                                StandingbookDTO::getCode,
                                StandingbookDTO::getStandingbookId
                        ));
                // 1. 添加下级计量器具的关联关系，从编码转成对应id
                // 2. 添加关联设备关联关系，从编码转成对应id
                // 3. 给所有计量器具添加环节，环节从汉字转成对应的



                return serverStandingbookList.size();
            }
        } catch (Exception e) {
            log.error("合法数据入库失败", e);
        }

        log.info("合法数据全部入库完成");
        return 100;
    }

}