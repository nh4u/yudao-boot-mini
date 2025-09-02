package cn.bitlinks.ems.module.power.service.doublecarbon;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.doublecarbon.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.doublecarbon.DoubleCarbonMappingDO;
import cn.bitlinks.ems.module.power.dal.dataobject.doublecarbon.DoubleCarbonSettingsDO;
import cn.bitlinks.ems.module.power.dal.mysql.doublecarbon.DoubleCarbonMappingMapper;
import cn.bitlinks.ems.module.power.dal.mysql.doublecarbon.DoubleCarbonSettingsMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Collections;


@Service
@Validated
public class DoubleCarbonServiceImpl implements DoubleCarbonService {
    @Resource
    private DoubleCarbonSettingsMapper doubleCarbonSettingsMapper;
    @Resource
    private DoubleCarbonMappingMapper doubleCarbonMappingMapper;

    @Override
    public DoubleCarbonSettingsRespVO getSettings() {
        DoubleCarbonSettingsDO settingsDO = doubleCarbonSettingsMapper.selectOne(null);
        return BeanUtils.toBean(settingsDO, DoubleCarbonSettingsRespVO.class);
    }


    @Override
    public void updSettings(DoubleCarbonSettingsUpdVO updVO) {
        doubleCarbonSettingsMapper.update(new LambdaUpdateWrapper<DoubleCarbonSettingsDO>()
                .set(DoubleCarbonSettingsDO::getUpdateFrequency, updVO.getUpdateFrequency())
                .set(DoubleCarbonSettingsDO::getUpdateFrequencyUnit, updVO.getUpdateFrequencyUnit())
                .eq(DoubleCarbonSettingsDO::getId, updVO.getId())
        );
    }

    @Override
    public void updMapping(DoubleCarbonMappingUpdVO updVO) {
        doubleCarbonMappingMapper.update(new LambdaUpdateWrapper<DoubleCarbonMappingDO>()
                .set(DoubleCarbonMappingDO::getDoubleCarbonCode, updVO.getDoubleCarbonCode())
                .eq(DoubleCarbonMappingDO::getId, updVO.getId())
        );
    }

    @Override
    public PageResult<DoubleCarbonMappingRespVO> getMappingPage(DoubleCarbonMappingPageReqVO pageReqVO) {
        PageResult<DoubleCarbonMappingRespVO> pageResult = new PageResult<>();
        PageResult<DoubleCarbonMappingDO> page = doubleCarbonMappingMapper.selectPage(pageReqVO);
        if (page == null) {
            pageResult.setTotal(0L);
            pageResult.setList(Collections.emptyList());
            return pageResult;
        }
        pageResult.setList(BeanUtils.toBean(page.getList(), DoubleCarbonMappingRespVO.class));
        pageResult.setTotal(page.getTotal());

        return pageResult;
    }

    @Override
    public void addMapping(String standingbookCode) {
        if (StringUtils.isEmpty(standingbookCode)) {
            return;
        }
        DoubleCarbonMappingDO mappingDO = new DoubleCarbonMappingDO();
        mappingDO.setStandingbookCode(standingbookCode);
        doubleCarbonMappingMapper.insert(mappingDO);
    }

    @Override
    public void delMapping(String standingbookCode) {
        if (StringUtils.isEmpty(standingbookCode)) {
            return;
        }
        doubleCarbonMappingMapper.delete(new LambdaQueryWrapperX<DoubleCarbonMappingDO>()
                .eq(DoubleCarbonMappingDO::getStandingbookCode, standingbookCode));
    }
}
