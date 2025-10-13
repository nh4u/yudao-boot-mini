package cn.bitlinks.ems.module.power.dal.mysql.sharefile;

import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.module.power.dal.dataobject.sharefile.ShareFileSettingsDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 服务设置 Mapper
 *
 * @author bitlinks
 */
@Mapper
public interface ShareFileSettingsMapper extends BaseMapperX<ShareFileSettingsDO> {

}