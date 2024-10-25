package cn.bitlinks.ems.module.power.service.labelconfig;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.power.controller.admin.labelconfig.vo.LabelConfigPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.labelconfig.vo.LabelConfigSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.labelconfig.LabelConfigDO;
import cn.bitlinks.ems.module.power.dal.mysql.labelconfig.LabelConfigMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;

import java.util.List;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.LABEL_CONFIG_NOT_EXISTS;

/**
 * 配置标签 Service 实现类
 *
 * @author bitlinks
 */
@Service
@Validated
public class LabelConfigServiceImpl implements LabelConfigService {

    @Resource
    private LabelConfigMapper labelConfigMapper;

    @Override
    public Long createLabelConfig(LabelConfigSaveReqVO createReqVO) {
        // 插入
        LabelConfigDO labelConfig = BeanUtils.toBean(createReqVO, LabelConfigDO.class);
        labelConfigMapper.insert(labelConfig);
        // 返回
        return labelConfig.getId();
    }

    @Override
    public void updateLabelConfig(LabelConfigSaveReqVO updateReqVO) {
        // 校验存在
        validateLabelConfigExists(updateReqVO.getId());
        // 更新
        LabelConfigDO updateObj = BeanUtils.toBean(updateReqVO, LabelConfigDO.class);
        labelConfigMapper.updateById(updateObj);
    }

    @Override
    public void deleteLabelConfig(Long id) {
        // 校验存在
        validateLabelConfigExists(id);
        // 删除
        labelConfigMapper.deleteById(id);
    }

    private void validateLabelConfigExists(Long id) {
        if (labelConfigMapper.selectById(id) == null) {
            throw exception(LABEL_CONFIG_NOT_EXISTS);
        }
    }

    @Override
    public LabelConfigDO getLabelConfig(Long id) {
        return labelConfigMapper.selectById(id);
    }

    @Override
    public PageResult<LabelConfigDO> getLabelConfigPage(LabelConfigPageReqVO pageReqVO) {


        return labelConfigMapper.selectPage(pageReqVO);
    }

    @Override
    public List<LabelConfigDO> getAllLabelConfigs() {
        return null;
    }


}