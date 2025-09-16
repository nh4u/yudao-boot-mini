package cn.bitlinks.ems.module.power.service.sync;

import cn.bitlinks.ems.module.power.controller.admin.doublecarbon.vo.SyncDoubleCarbonData;

import java.util.List;

/**
 * @author liumingqiang
 */
public interface SyncDoubleCarbonService {


    List<SyncDoubleCarbonData> getSyncDoubleCarbonData();

}
