package cn.bitlinks.ems.module.power.service.standingbook.label;

import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookLabelInfoDO;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.StandingbookLabelInfoMapper;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.text.StrPool;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author wangl
 * @date 2025年05月07日 15:48
 */
@Service("standingbookLabelInfoService")
@Validated
public class StandingbookLabelInfoServiceImpl implements StandingbookLabelInfoService {


    @Resource
    private StandingbookLabelInfoMapper standingbookLabelInfoMapper;

    @Override
    public List<StandingbookLabelInfoDO> getByLabelNames(List<String> labelNames) {

        return standingbookLabelInfoMapper.getByLabelNames(labelNames);
    }

    @Override
    public List<StandingbookLabelInfoDO> getByValues(List<String> values) {
        return standingbookLabelInfoMapper.getByValues(values);
    }

    @Override
    public List<StandingbookLabelInfoDO> getByStandingBookIds(List<Long> standingBookIdList) {
        LambdaQueryWrapper<StandingbookLabelInfoDO> wrapper = new LambdaQueryWrapper();
        wrapper.in(StandingbookLabelInfoDO::getStandingbookId, standingBookIdList);
        return standingbookLabelInfoMapper.selectList(wrapper);
    }

    @Override
    public List<StandingbookLabelInfoDO> getByStandingBookId(Long standingBookId) {
        return standingbookLabelInfoMapper.selectList(StandingbookLabelInfoDO::getStandingbookId, standingBookId);
    }

    @Override
    public List<StandingbookLabelInfoDO> getByValuesSelected(List<String> values) {
        return standingbookLabelInfoMapper.getByValuesSelected(values);
    }

    /**
     * 获取该标签绑定的台账
     *
     * @param labelValue
     * @return
     */
    @Override
    public List<StandingbookLabelInfoDO> getSelfByLabelValues(String labelValue) {
        LambdaQueryWrapper<StandingbookLabelInfoDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StandingbookLabelInfoDO::getValue, labelValue);
        List<StandingbookLabelInfoDO> list = standingbookLabelInfoMapper.selectList(wrapper);
        if (CollUtil.isNotEmpty(list)) {
            // 该标签有绑定台账 则直接返回
            return list;
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * 取该标签下一级的标签绑定信息
     *
     * @param labelValue
     * @return
     */
    @Override
    public List<StandingbookLabelInfoDO> getSubByLabelValues(String labelValue) {
        LambdaQueryWrapper<StandingbookLabelInfoDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StandingbookLabelInfoDO::getValue, labelValue);
        List<StandingbookLabelInfoDO> list = standingbookLabelInfoMapper.selectList(wrapper);

        if (CollUtil.isNotEmpty(list)) {
            // 只取第一级
            int length = labelValue.split(StrPool.COMMA).length;
            return list.stream().filter(s -> {
                String value = s.getValue();
                if (CharSequenceUtil.isNotEmpty(value)) {
                    int subValueLength = value.split(StrPool.COMMA).length;
                    // 长度相等才是直接子级
                    return length == subValueLength - 1;
                } else {
                    return false;
                }
            }).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * 取该标签下所有子级的标签绑定信息
     *
     * @param labelValue
     * @return
     */
    @Override
    public List<StandingbookLabelInfoDO> getAllSubByLabelValues(String labelValue) {
        LambdaQueryWrapper<StandingbookLabelInfoDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StandingbookLabelInfoDO::getValue, labelValue);

        // 方式一 取该标签下所有子级
        return standingbookLabelInfoMapper.selectList(wrapper);
    }

    @Override
    public List<StandingbookLabelInfoDO> getByLabelValues(String labelValue) {
        List<StandingbookLabelInfoDO> selfLabelInfoList = getSelfByLabelValues(labelValue);
        if (CollUtil.isNotEmpty(selfLabelInfoList)) {
            // 自己有 取自己的
            return selfLabelInfoList;
        }
        // 自己没有 取所有下级的
        return getAllSubByLabelValues(labelValue);
    }


}
