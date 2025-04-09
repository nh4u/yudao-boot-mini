package cn.bitlinks.ems.module.power.service.pricedetail;

import com.alibaba.nacos.client.naming.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import org.springframework.validation.annotation.Validated;

import java.util.*;
import java.util.stream.Collectors;

import cn.bitlinks.ems.module.power.controller.admin.pricedetail.vo.*;
import cn.bitlinks.ems.module.power.dal.dataobject.pricedetail.PriceDetailDO;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;

import cn.bitlinks.ems.module.power.dal.mysql.pricedetail.PriceDetailMapper;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;

/**
 * 单价详细 Service 实现类
 *
 * @author bitlinks
 */
@Service
@Validated
public class PriceDetailServiceImpl implements PriceDetailService {

    @Resource
    private PriceDetailMapper priceDetailMapper;

    @Override
    public Long createPriceDetail(PriceDetailSaveReqVO createReqVO) {
        // 插入
        PriceDetailDO priceDetail = BeanUtils.toBean(createReqVO, PriceDetailDO.class);
        priceDetailMapper.insert(priceDetail);
        // 返回
        return priceDetail.getId();
    }

    @Override
    public void updatePriceDetail(PriceDetailSaveReqVO updateReqVO) {
        // 校验存在
        validatePriceDetailExists(updateReqVO.getId());
        // 更新
        PriceDetailDO updateObj = BeanUtils.toBean(updateReqVO, PriceDetailDO.class);
        priceDetailMapper.updateById(updateObj);
    }

    @Override
    public void deletePriceDetail(Long id) {
        // 校验存在
        validatePriceDetailExists(id);
        // 删除
        priceDetailMapper.deleteById(id);
    }

    private void validatePriceDetailExists(Long id) {
        if (priceDetailMapper.selectById(id) == null) {
            throw exception(PRICE_DETAIL_NOT_EXISTS);
        }
    }

    @Override
    public PriceDetailDO getPriceDetail(Long id) {
        return priceDetailMapper.selectById(id);
    }

    @Override
    public PageResult<PriceDetailDO> getPriceDetailPage(PriceDetailPageReqVO pageReqVO) {
        return priceDetailMapper.selectPage(pageReqVO);
    }

    // 实现类新增
    @Override
    public List<PriceDetailDO> getDetailsByPriceId(Long priceId) {
        return priceDetailMapper.selectList(
                Wrappers.<PriceDetailDO>lambdaQuery()
                        .eq(PriceDetailDO::getPriceId, priceId)
        );
    }

    @Override
    public void deleteByPriceId(Long priceId) {
        priceDetailMapper.delete(
                Wrappers.<PriceDetailDO>lambdaQuery()
                        .eq(PriceDetailDO::getPriceId, priceId)
        );
    }

    @Override
    public Map<Long, List<PriceDetailDO>> getDetailsByPriceIds(List<Long> priceIds) {
        if (CollectionUtils.isEmpty(priceIds)) {
            return Collections.emptyMap();
        }
        // 查询并按 price_id 分组
        return priceDetailMapper.selectByPriceIds(priceIds).stream()
                .collect(Collectors.groupingBy(PriceDetailDO::getPriceId));
    }

}