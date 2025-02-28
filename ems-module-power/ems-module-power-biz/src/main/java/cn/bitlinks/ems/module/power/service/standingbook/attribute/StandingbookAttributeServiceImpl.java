package cn.bitlinks.ems.module.power.service.standingbook.attribute;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.attribute.vo.StandingbookAttributePageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.attribute.vo.StandingbookAttributeSaveReqVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.type.vo.StandingbookTypeListReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.attribute.StandingbookAttributeDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.type.StandingbookTypeDO;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.attribute.StandingbookAttributeMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.type.StandingbookTypeMapper;
import cn.bitlinks.ems.module.power.enums.ApiConstants;
import com.alibaba.excel.util.StringUtils;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.ArrayList;
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
    @Resource
    private StandingbookTypeMapper standingbookTypeMapper;

    @Autowired
    private ApplicationContext context;

    @Transactional
    @Override
    public Long createStandingbookAttribute(StandingbookAttributeSaveReqVO createReqVO) {
        // 插入
        StandingbookAttributeDO standingbookAttribute = BeanUtils.toBean(createReqVO, StandingbookAttributeDO.class);
        if ((standingbookAttribute.getId()==null)) {
            standingbookAttributeMapper.insert(standingbookAttribute);
        }else {
            standingbookAttributeMapper.updateById(standingbookAttribute);
        }
        // 返回
        return standingbookAttribute.getId();
    }

    @Transactional
    @Override
    public Long create(StandingbookAttributeSaveReqVO createReqVO) {
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

    @Transactional
    @Override
    public void updateStandingbookAttribute(StandingbookAttributeSaveReqVO updateReqVO) {
        // 校验存在
        validateStandingbookAttributeExists(updateReqVO.getId());
        // 更新
        StandingbookAttributeDO updateObj = BeanUtils.toBean(updateReqVO, StandingbookAttributeDO.class);
        standingbookAttributeMapper.updateById(updateObj);
    }

    @Transactional
    @Override
    public void update(StandingbookAttributeSaveReqVO updateReqVO) {
        // 更新
        StandingbookAttributeDO updateObj = BeanUtils.toBean(updateReqVO, StandingbookAttributeDO.class);
        LambdaUpdateWrapper<StandingbookAttributeDO> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(StandingbookAttributeDO::getCode, updateObj.getCode()).eq(StandingbookAttributeDO::getStandingbookId, updateObj.getStandingbookId());
        standingbookAttributeMapper.update(updateObj, updateWrapper);
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
    public List<StandingbookAttributeDO> getStandingbookAttributeByTypeId(Long typeId) {
        return standingbookAttributeMapper.selectTypeId(typeId);
    }

    @Override
    public PageResult<StandingbookAttributeDO> getStandingbookAttributePage(StandingbookAttributePageReqVO pageReqVO) {
        return standingbookAttributeMapper.selectPage(pageReqVO);
    }

    @Override
    public List<StandingbookDO> getStandingbook(List<StandingbookAttributePageReqVO> children, Long typeId) {
        return standingbookAttributeMapper.selectStandingbook(children, typeId);
    }

    @Override
    public void saveMultiple(List<StandingbookAttributeSaveReqVO> createReqVOs) {
        Long typeId = createReqVOs.get(0).getTypeId();
        StandingbookTypeDO standingbookType = standingbookTypeMapper.selectById(typeId);
        StandingbookAttributeService proxy = context.getBean(StandingbookAttributeService.class);
        proxy.deleteStandingbookAttributeByTypeId(typeId);
        for (StandingbookAttributeSaveReqVO createReqVO : createReqVOs) {
            proxy.createStandingbookAttribute(createReqVO);
        }
// 给子节点也添加新的属性
        StandingbookTypeListReqVO standingbookTypeListReqVO = new StandingbookTypeListReqVO();
        standingbookTypeListReqVO.setSuperId(typeId);
        List<StandingbookTypeDO> standingbookTypeList = standingbookTypeMapper.selectList(standingbookTypeListReqVO);
        if (standingbookTypeList == null || standingbookTypeList.size() == 0) {
            return;
        }
        for (StandingbookTypeDO standingbookTypeDO : standingbookTypeList) {
            Long sonId = standingbookTypeDO.getId();
            List<StandingbookAttributeDO> sonAttributeList = getStandingbookAttributeByTypeId(sonId);
            List<StandingbookAttributeSaveReqVO> standingbookAttributeSaveReqVOS = new ArrayList<>(createReqVOs);
            List<StandingbookAttributeSaveReqVO> sonNewList = new ArrayList<>();
            List<StandingbookAttributeSaveReqVO> sonNewSaveList = new ArrayList<>();
//新增的属性
            standingbookAttributeSaveReqVOS.forEach(son -> {
                son.setTypeId(sonId);
                String flag = "0";
                for (StandingbookAttributeDO sonAttribute : sonAttributeList) {
                    if (son.getCode().equals(sonAttribute.getCode())) {
                        flag = ("1");
                        break;
                    }
                }
                if ("0".equals(flag)) {
                    son.setNode(standingbookType.getName()).setAutoGenerated(ApiConstants.YES).setDescription("父节点属性自动生成");
                    sonNewList.add(son);
                }
            });
//减少的属性
            for (StandingbookAttributeDO sonAttribute : sonAttributeList) {
                String flag = "0";
                for (StandingbookAttributeSaveReqVO son : createReqVOs) {
                    if (son.getCode().equals(sonAttribute.getCode())) {
                        flag = ("1");
                        break;
                    }
                }
                if ("0".equals(flag)&& StringUtils.isNotBlank(sonAttribute.getDescription()) && sonAttribute.getDescription().contains("父节点属性自动生成")) {
                  continue;
                }

                StandingbookAttributeSaveReqVO bean = BeanUtils.toBean(sonAttribute, StandingbookAttributeSaveReqVO.class);
                bean.setId(null);
                sonNewSaveList.add(bean);
            }
            sonNewSaveList.addAll(sonNewList);
            saveMultiple(sonNewSaveList);
        }

    }

}
