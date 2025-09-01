package cn.bitlinks.ems.module.power.enums;


/**
 * power Redis Key 枚举类
 *
 * @author bitlinks
 */
public interface RedisKeyConstants {


    String STANDING_BOOK_TYPE_LIST = "power:standingbook:type:list";
    String STANDING_BOOK_TYPE_TREE = "power:standingbook:type:tree";

    String STANDING_BOOK_LIST = "power:standingbook:list";
    String STANDING_BOOK_DEVICE_CODE_LIST = "power:standingbook:device:list";
    String STANDING_BOOK_MEASUREMENT_CODE_LIST = "power:standingbook:measurement:list";

    /**
     * 有效时间默认为1小时，修改成10分钟
     */
    String STANDING_BOOK_CODE_KEYMAP = "power:standingbook:map#10m";
    /**
     * 台账id->数采配置
     */
    String STANDING_BOOK_ACQ_CONFIG_PREFIX = "power:standingbook:acq_config:%s";
    /**
     * 服务->io地址们
     */
    String STANDING_BOOK_SERVER_IO_CONFIG = "power:server_io_config";
    /**
     * 服务->设备们
     */
    String STANDING_BOOK_SERVER_DEVICE_CONFIG = "power:server_device_config";
}
