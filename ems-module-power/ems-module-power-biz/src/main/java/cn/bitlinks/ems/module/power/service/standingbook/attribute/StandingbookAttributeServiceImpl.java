package cn.bitlinks.ems.module.power.service.standingbook.attribute;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.attribute.vo.StandingbookAttributePageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.attribute.vo.StandingbookAttributeSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.attribute.StandingbookAttributeDO;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.attribute.StandingbookAttributeMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.STANDINGBOOK_ATTRIBUTE_NOT_EXISTS;

/**
 * 台账属性 Service 实现类
 *
 * @author bitlinks
 */
@Service
@Validated
public class StandingbookAttributeServiceImpl implements StandingbookAttributeService {
    @Resource
    private StandingbookAttributeMapper standingbookAttributeMapper;

    @Transactional
    @Override
    public Long createStandingbookAttribute(StandingbookAttributeSaveReqVO createReqVO) {
        // 插入
        StandingbookAttributeDO standingbookAttribute = BeanUtils.toBean(createReqVO, StandingbookAttributeDO.class);
        standingbookAttributeMapper.insert(standingbookAttribute);
        // 返回
        return standingbookAttribute.getId();
    }
    @Transactional
    @Override
    public void createStandingbookAttributeBatch(List<StandingbookAttributeDO> dos) {
         standingbookAttributeMapper.insertBatch(dos);
    }

    @Override
    public void updateStandingbookAttribute(StandingbookAttributeSaveReqVO updateReqVO) {
        // 校验存在
        validateStandingbookAttributeExists(updateReqVO.getId());
        // 更新
        StandingbookAttributeDO updateObj = BeanUtils.toBean(updateReqVO, StandingbookAttributeDO.class);
        standingbookAttributeMapper.updateById(updateObj);
    }

    @Override
    public void deleteStandingbookAttribute(Long id) {
        // 校验存在
        validateStandingbookAttributeExists(id);
        // 删除
        standingbookAttributeMapper.deleteById(id);
    }
    @Override
    public void deleteStandingbookAttributeByTypeId(Long typeId) {
        // 校验存在
        validateStandingbookAttributeExistsByTypeId(typeId);
        // 删除
        standingbookAttributeMapper.deleteTypeId(typeId);
    }
   @Override
    public void deleteStandingbookAttributeByStandingbookId(Long standingbookId) {
        // 校验存在
      validateStandingbookAttributeExistsByStandingbookId(standingbookId);
        // 删除
        standingbookAttributeMapper.deleteStandingbookId(standingbookId);
    }

    private void validateStandingbookAttributeExists(Long id) {
        if (standingbookAttributeMapper.selectById(id) == null) {
            throw exception(STANDINGBOOK_ATTRIBUTE_NOT_EXISTS);
        }
    }
    private void validateStandingbookAttributeExistsByStandingbookId(Long standingbookId) {
        if (standingbookAttributeMapper.selectStandingbookId(standingbookId) == null) {
            throw exception(STANDINGBOOK_ATTRIBUTE_NOT_EXISTS);
        }
    }

    private void validateStandingbookAttributeExistsByTypeId(Long typeId) {
        if (standingbookAttributeMapper.selectTypeId(typeId) == null) {
            throw exception(STANDINGBOOK_ATTRIBUTE_NOT_EXISTS);
        }
    }

    @Override
    public StandingbookAttributeDO getStandingbookAttribute(Long id) {
        return standingbookAttributeMapper.selectById(id);
    }

    @Override
    public List<StandingbookAttributeDO> getStandingbookAttributeByStandingbookId(Long standingbookId) {
        return standingbookAttributeMapper.selectStandingbookId(standingbookId);
    }

    @Override
    public PageResult<StandingbookAttributeDO> getStandingbookAttributePage(StandingbookAttributePageReqVO pageReqVO) {
        return standingbookAttributeMapper.selectPage(pageReqVO);
    }

}
