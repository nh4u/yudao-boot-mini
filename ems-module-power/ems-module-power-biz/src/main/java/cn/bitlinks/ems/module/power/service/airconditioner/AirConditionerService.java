package cn.bitlinks.ems.module.power.service.airconditioner;


import java.util.List;

public interface AirConditionerService {
    /**
     * 获取选项列表
     *
     * @return 返回一个包含字符串选项的List集合
     */
    List<String> getOptions();
}
