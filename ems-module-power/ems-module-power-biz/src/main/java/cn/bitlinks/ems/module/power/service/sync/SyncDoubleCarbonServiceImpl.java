package cn.bitlinks.ems.module.power.service.sync;

import cn.bitlinks.ems.framework.common.enums.DataTypeEnum;
import cn.bitlinks.ems.framework.common.util.date.LocalDateTimeUtils;
import cn.bitlinks.ems.module.power.controller.admin.doublecarbon.vo.DoubleCarbonMappingRespVO;
import cn.bitlinks.ems.module.power.controller.admin.doublecarbon.vo.SyncDoubleCarbonData;
import cn.bitlinks.ems.module.power.controller.admin.statistics.vo.UsageCostData;
import cn.bitlinks.ems.module.power.service.standingbook.StandingbookService;
import cn.bitlinks.ems.module.power.service.usagecost.UsageCostService;
import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


/**
 * @author liumingqiang
 */
@Service
@Slf4j
@Validated
public class SyncDoubleCarbonServiceImpl implements SyncDoubleCarbonService {
    @Resource
    private StandingbookService standingbookService;
    @Resource
    private UsageCostService usageCostService;


    @Override
    public List<SyncDoubleCarbonData> getSyncDoubleCarbonData(LocalDateTime startTime, LocalDateTime endTime) {

        List<SyncDoubleCarbonData> resultList = new ArrayList<>();

        List<DoubleCarbonMappingRespVO> effectiveSbIds = standingbookService.getEffectiveSbIds();

        List<Long> sbIds = effectiveSbIds
                .stream()
                .map(DoubleCarbonMappingRespVO::getStandingbookId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<UsageCostData> timeSbUsageList = usageCostService.getTimeSbUsageList(
                DataTypeEnum.DAY.getCode(),
                startTime,
                endTime,
                sbIds);
        Map<Long, List<UsageCostData>> sbTimeUsageMap = timeSbUsageList
                .stream()
                .collect(Collectors.groupingBy(UsageCostData::getStandingbookId));
        effectiveSbIds.forEach(
                e -> {
                    Long standingBookId = e.getStandingbookId();
                    List<UsageCostData> usageCostDataList = sbTimeUsageMap.get(standingBookId);
                    if (CollUtil.isNotEmpty(usageCostDataList)) {
                        usageCostDataList.forEach(u -> {
                            SyncDoubleCarbonData data = new SyncDoubleCarbonData();
                            data.setDoubleCarbonCode(e.getDoubleCarbonCode());
                            data.setUsage(u.getCurrentTotalUsage());
                            data.setAcqTimeLong(dealAcqTimeLong(u.getTime()));
                            resultList.add(data);
                        });
                    }
                }
        );

        return resultList;
    }

    private Long dealAcqTimeLong(String time) {
        return LocalDate.parse(time)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();

    }
}
