package cn.bitlinks.ems.module.power.service.standingbook;

import cn.bitlinks.ems.framework.common.pojo.CommonResult;
import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.module.infra.api.file.FileApi;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.attribute.vo.StandingbookAttributePageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.attribute.vo.StandingbookAttributeSaveReqVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.ScaledImageDataVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookRespVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.attribute.StandingbookAttributeDO;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.StandingbookMapper;
import cn.bitlinks.ems.module.power.enums.ApiConstants;
import cn.bitlinks.ems.module.power.service.standingbook.attribute.StandingbookAttributeService;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.*;
import org.imgscalr.Scalr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.STANDINGBOOK_NOT_EXISTS;

/**
 * 台账属性 Service 实现类
 *
 * @author bitlinks
 */
@Service("standingbookService")
@Validated
public class StandingbookServiceImpl implements StandingbookService {
    @Autowired
    private ApplicationContext context;
    @Resource
    private StandingbookMapper standingbookMapper;
    @Resource
    private StandingbookAttributeService standingbookAttributeService;
    @Resource
    private FileApi fileApi;
    @Override
    @Transactional
    public Long createStandingbook(StandingbookSaveReqVO createReqVO) {
        // 插入
        StandingbookDO standingbook = BeanUtils.toBean(createReqVO, StandingbookDO.class);
        standingbookMapper.insert(standingbook);
        // 返回
        if (createReqVO.getChildren() != null && createReqVO.getChildren().size() > 0) {
            createReqVO.getChildren().forEach(child -> {
                child.setStandingbookId(standingbook.getId());
                standingbookAttributeService.createStandingbookAttribute(child);
            });
        }
        return standingbook.getId();
    }

    @Override
    @Transactional
    public void updateStandingbook(StandingbookSaveReqVO updateReqVO) {
        // 校验存在
        validateStandingbookExists(updateReqVO.getId());
        // 更新
        StandingbookDO updateObj = BeanUtils.toBean(updateReqVO, StandingbookDO.class);
        standingbookMapper.updateById(updateObj);
        // 更新属性
        if (updateReqVO.getChildren() != null && updateReqVO.getChildren().size() > 0) {
            updateReqVO.getChildren().forEach(child -> standingbookAttributeService.updateStandingbookAttribute(child));
        }

    }

    @Override
    public void deleteStandingbook(Long id) {
        // 校验存在
        validateStandingbookExists(id);
        // 删除
        standingbookMapper.deleteById(id);
        // 删除属性
        try {
            standingbookAttributeService.deleteStandingbookAttributeByStandingbookId(id);
        } catch (Exception e) {
            // 忽略属性删除失败 因为可能不存在
            e.printStackTrace();
        }
    }

    private void validateStandingbookExists(Long id) {
        if (standingbookMapper.selectById(id) == null) {
            throw exception(STANDINGBOOK_NOT_EXISTS);
        }
    }

    @Override
    public StandingbookDO getStandingbook(Long id) {
        StandingbookDO standingbookDO = standingbookMapper.selectById(id);
        if (standingbookDO == null) {
            return null;
        } else {
            addChildAll(standingbookDO);
        }
        return standingbookDO;
    }

    void addChildAll(StandingbookDO standingbookDO) {
        standingbookDO.addChildAll(standingbookAttributeService.getStandingbookAttributeByStandingbookId(standingbookDO.getId()));
    }

    @Override
    public PageResult<StandingbookDO> getStandingbookPage(StandingbookPageReqVO pageReqVO) {
        PageResult<StandingbookDO> standingbookDOPageResult = standingbookMapper.selectPage(pageReqVO);
        standingbookDOPageResult.getList().forEach(this::addChildAll);
        return standingbookDOPageResult;
    }

    @Override
    public List<StandingbookDO> getStandingbookList(StandingbookPageReqVO pageReqVO) {
        List<StandingbookAttributePageReqVO> children = pageReqVO.getChildren();
        List<StandingbookDO> standingbookDOS = standingbookAttributeService.getStandingbook(children, pageReqVO.getTypeId());
        standingbookDOS.forEach(this::addChildAll);
        return standingbookDOS;
    }
    @Override
    public Object importStandingbook(MultipartFile file, StandingbookRespVO pageReqVO) {
        StandingbookService proxy = context.getBean(StandingbookService.class);
        // TODO 导入功能实现
        Long typeId = pageReqVO.getTypeId();
        //模板
        List<StandingbookAttributeDO> attributes = standingbookAttributeService.getStandingbookAttributeByTypeId(typeId);
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 2; i <= sheet.getLastRowNum(); i++) {
                List<StandingbookAttributeSaveReqVO> children = new ArrayList<>();
                StandingbookSaveReqVO saveReq  = new StandingbookSaveReqVO(children);
                Row row = sheet.getRow(i);
                if (row == null) continue;
                for (int j = 0; j <= attributes.size(); j++) {
                    Cell cell = row.getCell(j);
                    if (cell == null) continue;
                    StandingbookAttributeSaveReqVO attribute = new StandingbookAttributeSaveReqVO();
                    BeanUtils.copyProperties(attributes.get(j), attribute);
                    attribute.setStandingbookId(null);
                    if (!attribute.getFormat().equals(ApiConstants.FILE)) {
                        String cellValue = getCellValue(cell);
                        System.out.println(cellValue);
                        attribute.setValue(cellValue);
                    } else {
                       // TODO 读取图片文件
//                        if (cell.getDrawings() != null) {
//                            XSSFPicture picture = (XSSFPicture) cell.getDrawingPatriarch().getAllPictures().get(0);
//                            byte[] pictureData = picture.getPictureData().getData();
//                            // 处理图片数据
//                        }

                    }
                    children.add(attribute);
                }
                proxy.createStandingbook(saveReq);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return "success";
    }

    @Override
    public void exportStandingbookExcel(StandingbookPageReqVO pageReqVO, HttpServletResponse response) {
        List<StandingbookDO> list =getStandingbookList(pageReqVO);
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
                        CommonResult result = fileApi.getFile(standingbookAttributeDO.getFileId());
                        if (result == null || result.getData() == null)continue;
                        JSONObject file = (JSONObject) JSONUtil.parse(result.getData());
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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void template(Long typeId, HttpServletResponse response) {
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getCellValue(Cell cell) {
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
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
}
