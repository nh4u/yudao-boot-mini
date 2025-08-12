package cn.bitlinks.ems.module.power.service.report.electricity;

import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo.TransformerUtilizationSettingsOptionsVO;
import cn.bitlinks.ems.module.power.controller.admin.report.electricity.vo.TransformerUtilizationSettingsVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookDTO;
import cn.bitlinks.ems.module.power.dal.dataobject.report.electricity.TransformerUtilizationSettingsDO;
import cn.bitlinks.ems.module.power.dal.mysql.report.electricity.TransformerUtilizationSettingsMapper;
import cn.bitlinks.ems.module.power.service.standingbook.StandingbookService;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Validated
@Slf4j
public class TransformerUtilizationServiceImpl implements TransformerUtilizationService {
    @Resource
    private TransformerUtilizationSettingsMapper transformerUtilizationSettingsMapper;
    @Resource
    private StandingbookService standingbookService;

    private final String namePattern = "%s(%s)";
    @Override
    @Transactional
    public void updSettings(List<TransformerUtilizationSettingsVO> settings) {
        // 校验
        if (CollUtil.isEmpty(settings)) {
            return;
        }

        List<TransformerUtilizationSettingsDO> list = BeanUtils.toBean(settings, TransformerUtilizationSettingsDO.class);
        Map<Boolean, List<TransformerUtilizationSettingsDO>> grouped = list.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getId() != null,
                        Collectors.toList()
                ));

        List<TransformerUtilizationSettingsDO> updateList = grouped.get(true);
        List<TransformerUtilizationSettingsDO> insertList = grouped.get(false);

        if (CollUtil.isNotEmpty(updateList)) {
            transformerUtilizationSettingsMapper.updateBatch(updateList);
        }

        if (CollUtil.isNotEmpty(insertList)) {
            transformerUtilizationSettingsMapper.insertBatch(insertList);
        }
    }

    @Override
    public List<TransformerUtilizationSettingsVO> getSettings() {

        List<TransformerUtilizationSettingsDO> transformerUtilizationSettingsDOList = transformerUtilizationSettingsMapper
                .selectList(new LambdaQueryWrapper<TransformerUtilizationSettingsDO>()
                        .orderByAsc(TransformerUtilizationSettingsDO::getSort));
        if (CollUtil.isEmpty(transformerUtilizationSettingsDOList)) {
            return Collections.emptyList();
        }
        return BeanUtils.toBean(transformerUtilizationSettingsDOList, TransformerUtilizationSettingsVO.class);
    }


    @Override
    public List<TransformerUtilizationSettingsOptionsVO> transformerOptions() {

        // 已按 sort 排序
        List<TransformerUtilizationSettingsDO> transformerUtilizationSettingsDOList =
                transformerUtilizationSettingsMapper.selectList(new LambdaQueryWrapper<TransformerUtilizationSettingsDO>()
                        .orderByAsc(TransformerUtilizationSettingsDO::getSort));
        if (CollUtil.isEmpty(transformerUtilizationSettingsDOList)) {
            return Collections.emptyList();
        }
        // （key -> StandingbookDTO）
        Map<Long, StandingbookDTO> standingbookDTOMap = standingbookService.getStandingbookDTOMap();

        if (standingbookDTOMap == null || standingbookDTOMap.isEmpty()) {
            return Collections.emptyList();
        }

        List<TransformerUtilizationSettingsOptionsVO> result = new ArrayList<>();

        for (TransformerUtilizationSettingsDO settingsDO : transformerUtilizationSettingsDOList) {

            StandingbookDTO dto = standingbookDTOMap.get(settingsDO.getId());
            if (Objects.isNull(dto)) {
                continue;
            }
            TransformerUtilizationSettingsOptionsVO vo = new TransformerUtilizationSettingsOptionsVO();
            vo.setTransformerId(settingsDO.getTransformerId());
            vo.setTransformerName(String.format(namePattern,dto.getName(),dto.getCode()));
            result.add(vo);
        }

        return result;
    }
}
