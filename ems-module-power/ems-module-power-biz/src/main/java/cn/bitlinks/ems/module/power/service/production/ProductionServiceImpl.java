package cn.bitlinks.ems.module.power.service.production;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.externalapi.vo.ProductionPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.externalapi.vo.ProductionSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.production.ProductionDO;
import cn.bitlinks.ems.module.power.dal.mysql.production.ProductionMapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;

import java.util.Objects;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.EXTERNAL_API_NOT_EXISTS;


/**
 * @author liumingqiang
 */
@Service
@Validated
public class ProductionServiceImpl implements ProductionService {

    @Resource
    private ProductionMapper productionMapper;

    @Override
    public ProductionDO createProduction(ProductionSaveReqVO createReqVO) {

        ProductionDO old = productionMapper.selectOne(new LambdaQueryWrapperX<ProductionDO>()
                .eq(ProductionDO::getSize, createReqVO.getSize())
                .eq(ProductionDO::getTime, createReqVO.getTime()).last("limit 1"));

        if (Objects.isNull(old)) {
            // 转换请求对象到数据对象
            ProductionDO productionDO = BeanUtils.toBean(createReqVO, ProductionDO.class);
            // 插入数据库
            productionMapper.insert(productionDO);

            return productionDO;
        } else {

            old.setPlan(createReqVO.getPlan());
            old.setLot(createReqVO.getLot());
            old.setSize(createReqVO.getSize());

            productionMapper.updateById(old);

            // 返回记录
            return old;
        }

    }

    @Override
    public void updateExternalApi(ProductionSaveReqVO updateReqVO) {
        Long id = updateReqVO.getId();
        // Step 1: 校验凭证是否存在
        validateExternalApiExists(id);

        // Step 5: 更新凭证记录
        ProductionDO updateObj = BeanUtils.toBean(updateReqVO, ProductionDO.class);
        productionMapper.updateById(updateObj);
    }

    @Override
    public void deleteExternalApi(Long id) {
        // 删除
        productionMapper.deleteById(id);
    }

    @Override
    public ProductionDO getExternalApi(Long id) {
        return productionMapper.selectById(id);
    }

    @Override
    public PageResult<ProductionDO> getExternalApiPage(ProductionPageReqVO pageReqVO) {
        return productionMapper.selectPage(pageReqVO);
    }


    private void validateExternalApiExists(Long id) {
        if (productionMapper.selectById(id) == null) {
            throw exception(EXTERNAL_API_NOT_EXISTS);
        }
    }
}