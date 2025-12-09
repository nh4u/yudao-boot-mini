package cn.bitlinks.ems.module.power.excelstyle;

import com.alibaba.excel.write.handler.CellWriteHandler;
import com.alibaba.excel.write.handler.context.CellWriteHandlerContext;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 按“父级 + 当前列”层级合并的策略：
 * - 只对指定列 mergeColumns 生效
 * - 只合并 dataList 中的数据行（跳过表头行）
 * - 列 i 合并时，要求：
 * 1）当前行与上一行该列值相同
 * 2）并且 0..i-1 列（父级列）值也相同
 */
public class HierarchyMergeStrategy implements CellWriteHandler {

    /**
     * 数据区内容（与 doWrite 传入的 dataList 保持一致）
     */
    private final List<List<Object>> dataList;

    /**
     * 表头行数（headRowNumber），即从第几行开始是数据行
     * 你的 header 每列是 4 级：表单名称/统计标签/统计周期/末级 => headRowNumber = 4
     */
    private final int headRowNumber;

    /**
     * 需要进行合并的列索引
     */
    private final int[] mergeColumns;

    /**
     * 层级深度：0..(hierarchyDepth-1) 视为“父级标签列”
     * 用于限制子级在父级边界内合并
     */
    private final int hierarchyDepth;

    public HierarchyMergeStrategy(List<List<Object>> dataList,
                                  int headRowNumber,
                                  int[] mergeColumns,
                                  int hierarchyDepth) {
        this.dataList = dataList;
        this.headRowNumber = headRowNumber;
        this.mergeColumns = mergeColumns;
        this.hierarchyDepth = hierarchyDepth;
    }

    @Override
    public void afterCellDispose(CellWriteHandlerContext context) {
        int rowIndex = context.getRowIndex();
        int colIndex = context.getColumnIndex();

        // 只处理指定列
        if (Arrays.stream(mergeColumns).noneMatch(c -> c == colIndex)) {
            return;
        }

        // 跳过表头行
        if (rowIndex < headRowNumber) {
            return;
        }

        int dataRowIndex = rowIndex - headRowNumber;
        if (dataRowIndex <= 0 || dataRowIndex >= dataList.size()) {
            return;
        }

        List<Object> currentRow = dataList.get(dataRowIndex);
        List<Object> previousRow = dataList.get(dataRowIndex - 1);

        Object currentVal = getCellValueSafe(currentRow, colIndex);
        Object previousVal = getCellValueSafe(previousRow, colIndex);

        // 当前列值不同，不合并
        if (!Objects.equals(currentVal, previousVal)) {
            return;
        }

        // ===== 关键：校验父级列是否一致，避免“二级无”和“二级有”下的三级被错误合并 =====
        int parentMaxCol = Math.min(colIndex, hierarchyDepth); // 只把 label 列当父级
        for (int i = 0; i < parentMaxCol; i++) {
            Object curParent = getCellValueSafe(currentRow, i);
            Object preParent = getCellValueSafe(previousRow, i);
            if (!Objects.equals(curParent, preParent)) {
                return; // 父级不同，不应跨父级合并
            }
        }

        // 到这里说明当前行和上一行可以合并
        Sheet sheet = context.getWriteSheetHolder().getSheet();
        boolean merged = false;

        // 查找上一行是否已经有合并区域，如果有则扩展
        for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
            CellRangeAddress range = sheet.getMergedRegion(i);
            if (range.getFirstColumn() == colIndex &&
                    range.getLastColumn() == colIndex &&
                    range.getLastRow() == rowIndex - 1) {
                // 扩展到当前行
                range.setLastRow(rowIndex);
                merged = true;
                break;
            }
        }

        if (!merged) {
            // 创建新的合并区域：上一行 + 当前行
            CellRangeAddress newRange = new CellRangeAddress(
                    rowIndex - 1, rowIndex, colIndex, colIndex);
            sheet.addMergedRegion(newRange);
        }
    }

    private Object getCellValueSafe(List<Object> row, int colIndex) {
        if (colIndex < 0 || colIndex >= row.size()) {
            return null;
        }
        return row.get(colIndex);
    }
}

