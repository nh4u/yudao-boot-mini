package cn.bitlinks.ems.module.power.excelstyle;

import com.alibaba.excel.enums.CellDataTypeEnum;
import com.alibaba.excel.metadata.Head;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.util.MapUtils;
import com.alibaba.excel.write.metadata.holder.WriteSheetHolder;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.poi.ss.usermodel.Cell;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 中文是3字节 英文数字英文符号是1字节，所以纯英文、数字、英文符号时的时候看起来很挤
 *
 * @Title: ydme-ems
 * @description:
 * @Author: Mingqiang LIU
 * @Date 2025/09/08 10:11
 **/
public class PaddedColumnWidthStrategy extends LongestMatchColumnWidthStyleStrategy {
    private static final int MAX_COLUMN_WIDTH = 255;
    private final Map<Integer, Map<Integer, Integer>> cache = MapUtils.newHashMapWithExpectedSize(8);

    public PaddedColumnWidthStrategy() {
        // TODO document why this constructor is empty
    }

    @Override
    protected void setColumnWidth(WriteSheetHolder writeSheetHolder, List<WriteCellData<?>> cellDataList, Cell cell, Head head, Integer relativeRowIndex, Boolean isHead) {
        boolean needSetWidth = isHead || !CollectionUtils.isEmpty(cellDataList);
        if (needSetWidth) {
            Map<Integer, Integer> maxColumnWidthMap = this.cache.computeIfAbsent(writeSheetHolder.getSheetNo(), key -> new HashMap<>(16));
            Integer columnWidth = dataCellLength(cellDataList, cell, isHead);
            if (columnWidth >= 0) {
                if (columnWidth > MAX_COLUMN_WIDTH) {
                    columnWidth = MAX_COLUMN_WIDTH;
                }

                Integer maxColumnWidth = maxColumnWidthMap.get(cell.getColumnIndex());
                if (maxColumnWidth == null || columnWidth > maxColumnWidth) {
                    maxColumnWidthMap.put(cell.getColumnIndex(), columnWidth);
                    writeSheetHolder.getSheet().setColumnWidth(cell.getColumnIndex(), columnWidth * 256);
                }

            }
        }
    }

    private Integer dataCellLength(List<WriteCellData<?>> cellDataList, Cell cell, Boolean isHead) {

        int strLength;
        int byteLength;

        if (Boolean.TRUE.equals(isHead)) {
            strLength = cell.getStringCellValue().length();
            byteLength = cell.getStringCellValue().getBytes().length;
            return dealHeadLength(strLength, byteLength);
        } else {
            WriteCellData<?> cellData = cellDataList.get(0);
            CellDataTypeEnum type = cellData.getType();
            if (type == null) {
                return -1;
            } else {
                switch (type) {
                    case STRING:
                        strLength = cellData.getStringValue().length();
                        byteLength = cellData.getStringValue().getBytes().length;
                        break;
                    case BOOLEAN:
                        strLength = cellData.getBooleanValue().toString().length();
                        byteLength = cellData.getBooleanValue().toString().getBytes().length;
                        break;
                    case NUMBER:
                        strLength = cellData.getNumberValue().toString().length();
                        byteLength = cellData.getNumberValue().toString().getBytes().length;
                        break;
                    default:
                        return -1;
                }

                return dealCellLength(strLength, byteLength);
            }
        }
    }

    /**
     * 中文是3字节 英文数字英文符号是1字节 如果是纯英文数字和英文字符时候 就需要padding一下 目前是加4
     *
     * @param strLength
     * @param byteLength
     * @return
     */
    private int dealCellLength(int strLength, int byteLength) {
        return strLength == byteLength ? byteLength + 2 : byteLength;
    }

    /**
     * 中文是3字节 英文数字英文符号是1字节 如果是纯英文数字和英文字符时候 就需要padding一下 目前是加2
     * 表头需要单独处理 因为即使有中文有英文 还是会显示不全，需始终用padding一下
     *
     * @param strLength
     * @param byteLength
     * @return
     */
    private int dealHeadLength(int strLength, int byteLength) {
        return strLength * 3 != byteLength ? byteLength + 2 : byteLength;
    }
}