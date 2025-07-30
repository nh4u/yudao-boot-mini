package cn.bitlinks.ems.module.power.enums;


/**
 * power Redis Key 枚举类
 *
 * @author bitlinks
 */
public interface RedisKeyConstants {


    String STANDING_BOOK_TYPE_ID_NAME_MAP = "power:standingbook:type:id_name:map";
    String STANDING_BOOK_TYPE_TREE = "power:standingbook:type:tree";

    String STANDING_BOOK_MAP = "power:standingbook:map";
    String STANDING_BOOK_LIST = "power:standingbook:list";

    /**
     * 有效时间默认为1小时，修改成10分钟
     */
    String STANDING_BOOK_CODE_KEYMAP = "power:standingbook:map#10m";
}
