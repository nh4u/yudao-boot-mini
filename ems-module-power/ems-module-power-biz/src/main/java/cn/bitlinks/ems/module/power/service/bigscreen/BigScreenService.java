package cn.bitlinks.ems.module.power.service.bigscreen;

import cn.bitlinks.ems.module.power.controller.admin.bigscreen.vo.BigScreenParamReqVO;
import cn.bitlinks.ems.module.power.controller.admin.bigscreen.vo.BigScreenRespVO;

/**
 * 台账属性 Service 接口
 *
 * @author bitlinks
 */
public interface BigScreenService {

    BigScreenRespVO getBigScreenDetails(BigScreenParamReqVO paramVO);
}
