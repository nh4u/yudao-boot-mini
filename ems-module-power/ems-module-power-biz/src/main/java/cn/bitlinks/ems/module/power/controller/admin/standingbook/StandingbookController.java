package cn.bitlinks.ems.module.power.controller.admin.standingbook;

import cn.bitlinks.ems.framework.apilog.core.annotation.ApiAccessLog;
import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.infra.api.file.FileApi;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.ScaledImageDataVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookRespVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.attribute.StandingbookAttributeDO;
import cn.bitlinks.ems.module.power.enums.ApiConstants;
import cn.bitlinks.ems.module.power.service.standingbook.StandingbookService;
import cn.bitlinks.ems.module.power.service.standingbook.attribute.StandingbookAttributeService;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFDataValidation;
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.imgscalr.Scalr;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URLEncoder;
import java.util.List;

import static cn.bitlinks.ems.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.bitlinks.ems.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 台账")
@RestController
@RequestMapping("/power/standingbook")
@Validated
public class StandingbookController {
    @Resource
    private FileApi fileApi;
    @Resource
    private StandingbookService standingbookService;
    @Resource
    private StandingbookAttributeService standingbookAttributeService;

    @PostMapping("/create")
    @Operation(summary = "创建台账")
    @PreAuthorize("@ss.hasPermission('power:standingbook:create')")
    public CommonResult<Long> createStandingbook(@Valid  @RequestBody StandingbookSaveReqVO createReqVO) {
        return success(standingbookService.createStandingbook(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新台账")
    @PreAuthorize("@ss.hasPermission('power:standingbook:update')")
    public CommonResult<Boolean> updateStandingbook(@Valid @RequestBody StandingbookSaveReqVO updateReqVO) {
        standingbookService.updateStandingbook(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除台账")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('power:standingbook:delete')")
    public CommonResult<Boolean> deleteStandingbook(@RequestParam("id") Long id) {
        standingbookService.deleteStandingbook(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得台账")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('power:standingbook:query')")
    public CommonResult<StandingbookRespVO> getStandingbook(@RequestParam("id") Long id) {
        StandingbookDO standingbook = standingbookService.getStandingbook(id);
        return success(BeanUtils.toBean(standingbook, StandingbookRespVO.class));
    }
    @PostMapping("/list")
    @Operation(summary = "获得台账列表")
    @PreAuthorize("@ss.hasPermission('power:standingbook:query')")
    public CommonResult<List<StandingbookRespVO>> getStandingbookPage(@Valid @RequestBody StandingbookPageReqVO pageReqVO) {
        List<StandingbookDO> list = standingbookService.getStandingbookList(pageReqVO);
        return success(BeanUtils.toBean(list, StandingbookRespVO.class));
    }

    @PostMapping("/export-excel")
    @Operation(summary = "导出台账 Excel")
    @PreAuthorize("@ss.hasPermission('power:standingbook:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportStandingbookExcel(@Valid @RequestBody StandingbookPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        if (pageReqVO == null && pageReqVO.getTypeId() == null) {
            throw new IllegalArgumentException("参数不能为空");
        }
        List<StandingbookDO> list = standingbookService.getStandingbookList(pageReqVO);
        Long typeId = pageReqVO.getTypeId();
        List<StandingbookAttributeDO> attributes = standingbookAttributeService.getStandingbookAttributeByTypeId(typeId);
        if (attributes == null || attributes.isEmpty()) {
            throw new IllegalArgumentException("台账属性不能为空");
        }
        // 导出 Excel
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("数据");
            // 第一行
            Row firstRow = sheet.createRow(0);
            Cell templateNumberCell = firstRow.createCell(0);
            templateNumberCell.setCellValue("模板编号: ");
            Cell templateNumberCell1 = firstRow.createCell(1);
            templateNumberCell1.setCellValue(typeId);
            // 创建表头
            Row headerRow = sheet.createRow(1);
            for (int i = 0; i < attributes.size(); i++) {
                sheet.setColumnWidth(i, 5000);
                StandingbookAttributeDO column = attributes.get(i);
//                 Cell cell = headerRow.createCell(i+1); // 从第二列开始
                Cell cell = headerRow.createCell(i); // 从第1列开始
                cell.setCellValue(column.getName());

                // 设置单元格样式
                CellStyle style = workbook.createCellStyle();
                cell.setCellStyle(style);
                if (ApiConstants.YES.equals(column.getIsRequired())) {
                    style.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
                    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                }else {
                    style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                }
            }
            for (int i = 0; i < list.size(); i++) {
                Row dataRow = sheet.createRow(i + 2);
                dataRow.setHeightInPoints(50); // 设置行高
                List<StandingbookAttributeDO> children = list.get(i).getChildren();
                for (int j =0; j < children.size(); j++) {
                    StandingbookAttributeDO standingbookAttributeDO = children.get(j);
                    Cell cell = dataRow.createCell(j); // 从第1列开始
                    if (ApiConstants.FILE.equals(standingbookAttributeDO.getFormat())){
//                        Long fileId1 = standingbookAttributeDO.getFileId();
                        CommonResult result = fileApi.getFile(standingbookAttributeDO.getFileId());
                        if (result == null || result.getData() == null)continue;
                        JSONObject file = (JSONObject) JSONUtil.parse(result.getData());
//                        FileCreateReqDTO createReqDTO=new FileCreateReqDTO();
//                        createReqDTO.setConfigId(Long.valueOf(file.getStr("configId"))).setPath("path");
                        CommonResult<byte[]> fileId = fileApi.getFileContent(Long.valueOf(file.getStr("configId")),file.getStr("path"));
                        if (fileId == null || fileId.getData() == null)continue;
                        byte[] bytes = fileId.getData();
                        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
                        // 缩放图片
                        ScaledImageDataVO scaledImageData = scaleImage(image);

                        //将缩放后的图片转换为bytes
                        byte[] imageBytes = processImageToBytes(scaledImageData.getScaledImage());

                        int pictureIdx = workbook.addPicture(imageBytes, Workbook.PICTURE_TYPE_PNG);
                        CreationHelper helper = workbook.getCreationHelper();
                        // 创建绘图对象
                        Drawing<?> drawing = sheet.createDrawingPatriarch();

                        // 创建一个锚点，指定图片的位置
//                        XSSFClientAnchor anchor = new XSSFClientAnchor(0, 0, 0, 0, i + 2, j, i + 2+1, j+1);

                        // 定位图片 (行1, 列1, 宽度和高度可根据需要调整)

                        ClientAnchor anchor = helper.createClientAnchor();
                        anchor.setCol1(j); // 列号从0开始，1表示第一列
                        anchor.setRow1(i + 2); // 行号从0开始，1表示第一行
                        anchor.setCol2(j); // 列号从0开始，1表示第一列
                        anchor.setRow2(i + 2); // 行号从0开始，1表示第一行
                        // 插入图片
                        Picture pict = drawing.createPicture(anchor, pictureIdx);
                        pict.resize(); // 调整图片大小以适应单元格
                    }else {
                        cell.setCellValue(standingbookAttributeDO.getValue());
                    }
                }
            }
            // 输出到文件

            response.reset();
            response.setContentType("application/octet-stream; charset=utf-8");
            response.setHeader("Content-Disposition", "attachment; filename="+ URLEncoder.encode("模板编号-" + typeId + "-台账导出.xlsx","UTF-8"));
            OutputStream os = response.getOutputStream();
            workbook.write(os);
            os.flush();
            os.close();
//            try (FileOutputStream fileOut = new FileOutputStream("D:\\破烂\\项目\\"+typeId+"-template.xlsx")) {
//                workbook.write(fileOut);
//            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 缩放图片
     */
    private static ScaledImageDataVO scaleImage(BufferedImage targetImage) {
        int desiredWidth = 50; // 图片宽度
        double aspectRatio = (double) targetImage.getHeight() / targetImage.getWidth();
        int desiredHeight = (int) (desiredWidth * aspectRatio); // 保持纵横比

        BufferedImage scaledImage = Scalr.resize(targetImage, Scalr.Method.QUALITY, desiredWidth, desiredHeight);
        return new ScaledImageDataVO(desiredHeight, scaledImage);
    }

    /**
     * 图片转bytes
     */
    private static byte[] processImageToBytes(BufferedImage targetImage) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            ImageIO.write(targetImage, "png", stream);
            return stream.toByteArray();
        } catch (IOException e) {
            return new byte[0];
        }
    }

     @GetMapping("/export-excel-template")
    @Operation(summary = "导出台账导入模板 Excel")
    @PreAuthorize("@ss.hasPermission('power:standingbook:export')")
     @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @ApiAccessLog(operateType = EXPORT)
    public void template(@RequestParam("typeId") Long typeId, HttpServletResponse response) throws IOException {
        if (typeId == null) {
            throw new IllegalArgumentException("台账编号不能为空");
        }
         List<StandingbookAttributeDO> attributes = standingbookAttributeService.getStandingbookAttributeByTypeId(typeId);
         if (attributes == null || attributes.isEmpty()) {
             throw new IllegalArgumentException("台账属性不能为空");
         }
         try (XSSFWorkbook workbook = new XSSFWorkbook()) {
             XSSFSheet sheet = workbook.createSheet("数据模板");

             // 第一行
             Row firstRow = sheet.createRow(0);
             Cell templateNumberCell = firstRow.createCell(0);
             templateNumberCell.setCellValue("模板编号: ");
            Cell templateNumberCell1 = firstRow.createCell(1);
             templateNumberCell1.setCellValue(typeId);

             // 合并第二、三、四、五列
             sheet.addMergedRegion(new CellRangeAddress(0, 0, 2, 5));
             Cell hintCell = firstRow.createCell(2);
             hintCell.setCellValue("表头黄色的为必填项，请勿修改模板编号，否则无法导入数据。请从第三行开始填写数据。");
             CellStyle s = workbook.createCellStyle();
             s.setFillForegroundColor(IndexedColors.ORANGE.getIndex());
             s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
             hintCell.setCellStyle(s);
             // 创建表头
             Row headerRow = sheet.createRow(1);
//             Cell headerCell = headerRow.createCell(0);
//             headerCell.setCellValue("台账名称");
             for (int i = 0; i < attributes.size(); i++) {
                 StandingbookAttributeDO column = attributes.get(i);
                 sheet.setColumnWidth(i, 5000);
//                 Cell cell = headerRow.createCell(i+1); // 从第二列开始
                 Cell cell = headerRow.createCell(i); // 从第1列开始
                 cell.setCellValue(column.getName());
                 // 设置单元格样式
                 CellStyle style = workbook.createCellStyle();
                 if (ApiConstants.YES.equals(column.getIsRequired())) {
                     style.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
                     style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                 }
//                 CellRangeAddressList addressList = new CellRangeAddressList(2, 1000, i+1 , i+1);
                 CellRangeAddressList addressList = new CellRangeAddressList(2, 1000, i , i);
                 // 根据format设置数据验证
                if (ApiConstants.SELECT.equals(column.getFormat())) {
                     XSSFDataValidationHelper validationHelper = new XSSFDataValidationHelper(sheet);
                     String[] options = column.getOptions().split(";");
                     DataValidationConstraint constraint = validationHelper.createExplicitListConstraint(options);
                     XSSFDataValidation validation = (XSSFDataValidation) validationHelper.createValidation(constraint,addressList);
                     sheet.addValidationData(validation);
                 }
                 cell.setCellStyle(style);
             }
             // 输出
             response.setContentType("application/octet-stream; charset=utf-8");
             response.setHeader("Content-Disposition", "attachment; filename="+ URLEncoder.encode("模板编号-" + typeId + "-台账导入模板.xlsx","UTF-8"));

             OutputStream os = response.getOutputStream();
             workbook.write(os);
             os.flush();
             os.close();
             try (FileOutputStream fileOut = new FileOutputStream("D:\\破烂\\项目\\"+typeId+"-template.xlsx")) {
                 workbook.write(fileOut);
             }
         }
    }

}
