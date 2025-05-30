package cn.bitlinks.ems.framework.common.util.opcda;

import cn.hutool.core.date.DateUtil;
import org.jinterop.dcom.common.JIException;
import org.jinterop.dcom.core.JIArray;
import org.jinterop.dcom.core.JIVariant;
import org.openscada.opc.lib.da.Group;
import org.openscada.opc.lib.da.Item;
import org.openscada.opc.lib.da.ItemState;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;


public class OpcUtil {
    private OpcUtil() {
    }

    /**
     * 读单个值
     */
    public static String readValue(Item item) {
        try {
            ItemState state = item.read(true);
            return getValue(state.getValue());
        } catch (JIException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 读一组值，对于读取异常的点位会返回null值
     */
    public static Map<String, ItemStatus> readValues(Group group, List<String> tags) {
        //添加到group中，如果添加失败则添加null
        List<Item> items = tags.stream().map(tag -> {
            try {
                return group.addItem(tag);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }).collect(Collectors.toList());

        Map<String, ItemStatus> resultMap = new HashMap<>();
        try {
            //读取所有的值，过滤null值，否则会出异常
            Map<Item, ItemState> map = group.read(true,
                    items.stream().filter(Objects::nonNull).toArray(Item[]::new));
            //解析
            for (Item item : items) {
                if (item == null) {
                    continue;
                }
                ItemState itemState = map.get(item);
                if (itemState == null) {
                    continue;
                }
                String value = getValue(itemState.getValue());
                if (value == null) {
                    continue;
                }
                ItemStatus itemStatus = new ItemStatus();
                itemStatus.setValue(value);
                itemStatus.setTime(DateUtil.toLocalDateTime(itemState.getTimestamp()));
                resultMap.put(item.getId(), itemStatus);
            }
        } catch (JIException e) {
            e.printStackTrace();
        }
        return resultMap;
    }

    /**
     * 如果是 bool、string、short、int等直接返回字符串；
     * 如果是 long 类型的数组,返回数字内容间加点，对应 long，数组，大小为6
     * 如果是 float 类型的数组,返回数字内容间加逗号，对应 float，数组，大小为20
     */
    private static String getValue(JIVariant variant) {
        System.out.println("-----------------" + variant.toString());
        try {
            int type = variant.getType();
            //Boolean
            if (type == JIVariant.VT_BOOL) {
                boolean value = variant.getObjectAsBoolean();
                return String.valueOf(value);
            }
            //String
            else if (type == JIVariant.VT_BSTR) {
                return variant.getObjectAsString().getString();
            }
            //Word DWord
            else if (type == JIVariant.VT_UI2 || type == JIVariant.VT_UI4) {
                Number value = variant.getObjectAsUnsigned().getValue();
                return String.valueOf(value);
            }
            //Sort
            else if (type == JIVariant.VT_I2) {
                short value = variant.getObjectAsShort();
                return String.valueOf(value);
            }
            //Float
            else if (type == JIVariant.VT_R4) {
                float value = variant.getObjectAsFloat();
                return String.valueOf(value);
            }
            //long 类型的数组
            else if (type == 8195) {
                JIArray jarr = variant.getObjectAsArray();
                Integer[] arr = (Integer[]) jarr.getArrayInstance();
                StringBuilder value = new StringBuilder();
                for (Integer i : arr) {
                    value.append(i).append(".");
                }
                String res = value.substring(0, value.length() - 1);
                // "25.36087601.1.1.18.36"-->"25.36087601.01.0001.18.36"
                String[] array = res.split("[.]");
                return array[0] + "." + array[1] + "." + new DecimalFormat("00").format(Long.valueOf(array[2]))
                        + "." + new DecimalFormat("0000").format(Long.valueOf(array[3])) + "." + array[4] + "."
                        + array[5];
            }
            //float 类型的数组
            else if (type == 8196) {
                JIArray jarr = variant.getObjectAsArray();
                Float[] arr = (Float[]) jarr.getArrayInstance();
                StringBuilder value = new StringBuilder();
                for (Float f : arr) {
                    value.append(f).append(",");
                }
                return value.substring(0, value.length() - 1);
            }
            //其他类型
            else {
                Object value = variant.getObject();
                return String.valueOf(value);
            }
        } catch (JIException e) {
            e.printStackTrace();
        }
        return null;
    }


}
