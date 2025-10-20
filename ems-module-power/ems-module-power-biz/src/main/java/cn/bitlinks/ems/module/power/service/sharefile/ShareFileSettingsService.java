package cn.bitlinks.ems.module.power.service.sharefile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 服务设置 Service 接口
 *
 * @author bitlinks
 */
public interface ShareFileSettingsService {


    void dealFile() throws IOException;


    Map<String, List<Map<String, Object>>> testShareFile() throws IOException;
}