package cn.bitlinks.ems.module.power.dal.mysql.copsettings;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.copsettings.vo.CopSettingsPageReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.copsettings.CopSettingsDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CopSettingsMapper extends BaseMapperX<CopSettingsDO> {

    default PageResult<CopSettingsDO> selectPage(CopSettingsPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CopSettingsDO>()
                .likeIfPresent(CopSettingsDO::getParamCnName, reqVO.getParamCnName())
                .eqIfPresent(CopSettingsDO::getCopType, reqVO.getCopType())
                .eqIfPresent(CopSettingsDO::getParam, reqVO.getParam())
                .eqIfPresent(CopSettingsDO::getDataFeature, reqVO.getDataFeature())
                .eqIfPresent(CopSettingsDO::getStandingbookId, reqVO.getStandingbookId())
                .orderByAsc(CopSettingsDO::getCreateTime));
    }

    /**
     * 根据copType获取所有cop参数数据
     *
     * @param copType 低温冷机 LTC,低温系统 LTS,中温冷机 MTC,中温系统 MTS
     * @return
     */
    default List<CopSettingsDO> getCopSettingsListByCopType(String copType) {
        return selectList(new LambdaQueryWrapperX<CopSettingsDO>()
                .eqIfPresent(CopSettingsDO::getCopType, copType)
                .orderByAsc(CopSettingsDO::getCreateTime));
    }
}