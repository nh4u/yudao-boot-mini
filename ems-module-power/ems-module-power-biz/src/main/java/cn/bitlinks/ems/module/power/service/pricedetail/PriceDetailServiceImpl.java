package cn.bitlinks.ems.module.power.service.pricedetail;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.module.power.controller.admin.pricedetail.vo.PriceDetailPageReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.pricedetail.PriceDetailDO;
import cn.bitlinks.ems.module.power.dal.mysql.pricedetail.PriceDetailMapper;
import com.alibaba.nacos.client.naming.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.PRICE_DETAIL_NOT_EXISTS;

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