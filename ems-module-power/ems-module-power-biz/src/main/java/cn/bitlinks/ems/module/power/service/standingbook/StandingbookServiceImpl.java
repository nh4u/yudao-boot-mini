package cn.bitlinks.ems.module.power.service.standingbook;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.attribute.vo.StandingbookAttributePageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.StandingbookMapper;
import cn.bitlinks.ems.module.power.service.standingbook.attribute.StandingbookAttributeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.List;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.STANDINGBOOK_NOT_EXISTS;

/**
 * 台账属性 Service 实现类
 *
 * @author bitlinks
 */
@Service("standingbookService")
@Validated
public class StandingbookServiceImpl implements StandingbookService {

    @Resource
    private StandingbookMapper standingbookMapper;
    @Resource
    private StandingbookAttributeService standingbookAttributeService;

    @Override
    @Transactional
    public Long createStandingbook(StandingbookSaveReqVO createReqVO) {
        // 插入
        StandingbookDO standingbook = BeanUtils.toBean(createReqVO, StandingbookDO.class);
        standingbookMapper.insert(standingbook);
        // 返回
        if (createReqVO.getChildren() != null && createReqVO.getChildren().size() > 0) {
            createReqVO.getChildren().forEach(child -> {
                child.setStandingbookId(standingbook.getId());
                standingbookAttributeService.createStandingbookAttribute(child);
            });
        }
        return standingbook.getId();
    }

    @Override
    @Transactional
    public void updateStandingbook(StandingbookSaveReqVO updateReqVO) {
        // 校验存在
        validateStandingbookExists(updateReqVO.getId());
        // 更新
        StandingbookDO updateObj = BeanUtils.toBean(updateReqVO, StandingbookDO.class);
        standingbookMapper.updateById(updateObj);
        // 更新属性
        if (updateReqVO.getChildren() != null && updateReqVO.getChildren().size() > 0) {
            updateReqVO.getChildren().forEach(child -> standingbookAttributeService.updateStandingbookAttribute(child));
        }

    }

    @Override
    public void deleteStandingbook(Long id) {
        // 校验存在
        validateStandingbookExists(id);
        // 删除
        standingbookMapper.deleteById(id);
        // 删除属性
        try {
            standingbookAttributeService.deleteStandingbookAttributeByStandingbookId(id);
        } catch (Exception e) {
            // 忽略属性删除失败 因为可能不存在
            e.printStackTrace();
        }
    }

    private void validateStandingbookExists(Long id) {
        if (standingbookMapper.selectById(id) == null) {
            throw exception(STANDINGBOOK_NOT_EXISTS);
        }
    }

    @Override
    public StandingbookDO getStandingbook(Long id) {
        StandingbookDO standingbookDO = standingbookMapper.selectById(id);
        if (standingbookDO == null) {
            return null;
        } else {
            addChildAll(standingbookDO);
        }
        return standingbookDO;
    }

    void addChildAll(StandingbookDO standingbookDO) {
        standingbookDO.addChildAll(standingbookAttributeService.getStandingbookAttributeByStandingbookId(standingbookDO.getId()));
    }

    @Override
    public PageResult<StandingbookDO> getStandingbookPage(StandingbookPageReqVO pageReqVO) {
        PageResult<StandingbookDO> standingbookDOPageResult = standingbookMapper.selectPage(pageReqVO);
        standingbookDOPageResult.getList().forEach(this::addChildAll);
        return standingbookDOPageResult;
    }

    @Override
    public List<StandingbookDO> getStandingbookList(StandingbookPageReqVO pageReqVO) {
        List<StandingbookAttributePageReqVO> children = pageReqVO.getChildren();
        List<StandingbookDO> standingbookDOS=standingbookAttributeService.getStandingbook(children,pageReqVO.getTypeId());
        standingbookDOS.forEach(this::addChildAll);
        return standingbookDOS;
    }

}
