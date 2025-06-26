package cn.bitlinks.ems.module.power.dal.mysql.standingbook.reportcod;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

import cn.bitlinks.ems.framework.mybatis.core.mapper.BaseMapperX;
import cn.bitlinks.ems.module.power.controller.admin.additionalrecording.vo.HeaderCodeMappingVO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.reportcod.HeaderCodeMappingDO;

/**
 * @author wangl
 * @date 2025年06月20日 17:09
 */
@Mapper
public interface HeaderCodeMappingMapper extends BaseMapperX<HeaderCodeMappingDO> {

    List<HeaderCodeMappingVO> selectByHeaderCode(@Param("headers") List<String> headers);
}
