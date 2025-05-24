package cn.bitlinks.ems.module.acquisition.api.collectrawdata;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.acquisition.api.collectrawdata.dto.CollectRawDataDTO;
import cn.bitlinks.ems.module.acquisition.dal.dataobject.collectrawdata.CollectRawDataDO;
import cn.bitlinks.ems.module.acquisition.service.collectrawdata.CollectRawDataService;
import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;

@Slf4j
@RestController // 提供 RESTful API 接口，给 Feign 调用
@Validated
public class CollectRawDataApiImpl implements CollectRawDataApi {
    @Resource
    private CollectRawDataService collectRawDataService;

    @Override
    public CommonResult<List<CollectRawDataDTO>> getCollectRawDataListByStandingBookIds(List<Long> standingBookIds) {
        List<CollectRawDataDO> collectRawDataDOList =
                collectRawDataService.selectLatestByStandingbookIds(standingBookIds);
        if (CollUtil.isEmpty(collectRawDataDOList)) {
            return success(Collections.emptyList());
        }
        return success(BeanUtils.toBean(collectRawDataDOList, CollectRawDataDTO.class));

    }
}
