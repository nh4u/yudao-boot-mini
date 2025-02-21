package cn.bitlinks.ems.module.power.service.standingbook;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.infra.api.file.FileApi;
import cn.bitlinks.ems.module.power.controller.admin.deviceassociationconfiguration.vo.AssociationData;
import cn.bitlinks.ems.module.power.controller.admin.deviceassociationconfiguration.vo.StandingbookWithAssociations;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.attribute.vo.StandingbookAttributePageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.attribute.vo.StandingbookAttributeSaveReqVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.type.vo.StandingbookTypeListReqVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.ScaledImageDataVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookRespVO;
import cn.bitlinks.ems.module.power.controller.admin.standingbook.vo.StandingbookSaveReqVO;
import cn.bitlinks.ems.module.power.dal.dataobject.deviceassociationconfiguration.DeviceAssociationConfigurationDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.StandingbookDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.attribute.StandingbookAttributeDO;
import cn.bitlinks.ems.module.power.dal.dataobject.standingbook.type.StandingbookTypeDO;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.StandingbookMapper;
import cn.bitlinks.ems.module.power.dal.mysql.standingbook.type.StandingbookTypeMapper;
import cn.bitlinks.ems.module.power.enums.ApiConstants;
import cn.bitlinks.ems.module.power.enums.CommonConstants;
import cn.bitlinks.ems.module.power.enums.ErrorCodeConstants;
import cn.bitlinks.ems.module.power.service.deviceassociationconfiguration.DeviceAssociationConfigurationService;
import cn.bitlinks.ems.module.power.service.labelconfig.LabelConfigService;
import cn.bitlinks.ems.module.power.service.standingbook.attribute.StandingbookAttributeService;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.JsonNode;
//import com.sun.xml.internal.bind.v2.TODO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFDataValidation;
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
import java.util.*;
import java.util.stream.Collectors;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.framework.common.util.json.JsonUtils.objectMapper;
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
    private LabelConfigService labelService;

    @Resource
    private StandingbookTypeMapper standingbookTypeMapper;

    @Resource
    private DeviceAssociationConfigurationService deviceAssociationConfigurationService;

    @Resource
    private FileApi fileApi;

    @Transactional
    public Long create(StandingbookSaveReqVO createReqVO) {
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
    public Long count(Long typeId) {

        // 对应类型【重点设备、计量器具、其他设备】下的所有类型id
        List<Long> typeIdList;
        if (typeId.equals(CommonConstants.OTHER_EQUIPMENT_ID)) {
            // 查询对应所有类型id
             typeIdList = standingbookTypeMapper.selectList(new LambdaQueryWrapperX<StandingbookTypeDO>()
                            .ne(StandingbookTypeDO::getTopType, CommonConstants.MEASUREMENT_INSTRUMENT_ID)
                            .ne(StandingbookTypeDO::getTopType, CommonConstants.KEY_EQUIPMENT_ID))
                    .stream()
                    .map(StandingbookTypeDO::getId)
                    .collect(Collectors.toList());
        } else {

            // 差对应id
            typeIdList = standingbookTypeMapper.selectList(new LambdaQueryWrapperX<StandingbookTypeDO>()
                            .eq(StandingbookTypeDO::getTopType, typeId))
                    .stream()
                    .map(StandingbookTypeDO::getId)
                    .collect(Collectors.toList());
        }
        return standingbookMapper.selectCount(new LambdaQueryWrapperX<StandingbookDO>()
                .in(StandingbookDO::getTypeId,typeIdList));
    }

    @Override
    public List<StandingbookDO> listByBaseTypeId(Map<String, String> pageReqVO ) {
        if (!pageReqVO.containsKey("topType")) {
            throw exception(ErrorCodeConstants.STANDINGBOOK_TYPE_NOT_EXISTS);
        }
        String topType = pageReqVO.get("topType");
        StandingbookTypeListReqVO listReqVO = new StandingbookTypeListReqVO();
        listReqVO.setTopType(String.valueOf(topType));
        List<StandingbookTypeDO> standingbookTypeDOS = standingbookTypeMapper.selectList(listReqVO);
        if (standingbookTypeDOS == null || standingbookTypeDOS.size() == 0) {
            throw exception(ErrorCodeConstants.STANDINGBOOK_NOT_EXISTS);
        }
        List<StandingbookDO> result =new ArrayList<>();
        pageReqVO.remove("topType");
        standingbookTypeDOS.forEach(standingbookTypeDO -> {
            Long sonId = standingbookTypeDO.getId();
            pageReqVO.put("typeId",String.valueOf(sonId));
            List<StandingbookDO> standingbookDOS = getStandingbookList(pageReqVO);
            result.addAll(standingbookDOS);
        });
        return result;
    }

    @Override
    @Transactional
    public Long createStandingbook(Map <String,String>  createReqVO) {
        // 插入
        if (!createReqVO.containsKey("typeId")) {
            throw exception(ErrorCodeConstants.STANDINGBOOK_TYPE_NOT_EXISTS);
        }
        String typeId = createReqVO.get("typeId");
        Long aLong = Long.valueOf(typeId);
        List<StandingbookAttributeDO> standingbookAttributeByTypeId = standingbookAttributeService.getStandingbookAttributeByTypeId(aLong);
        StandingbookDO standingbook = new StandingbookDO();
        standingbook.setTypeId(aLong);
        standingbook.setLabelInfo(createReqVO.get("labelInfo"));
        standingbookMapper.insert(standingbook);
        // 使用 entrySet() 遍历键和值
        List<StandingbookAttributeSaveReqVO> children= new ArrayList<>();
        for (Map.Entry<String, String> entry : createReqVO.entrySet()) {
            System.out.println("Key: " + entry.getKey() + ", Value: " + entry.getValue());
            String key = entry.getKey();
            String value = entry.getValue();
            if (!entry.getKey() .equals("typeId")  &&  !entry.getKey().equals("labelInfo")){
                StandingbookAttributeSaveReqVO attribute = new StandingbookAttributeSaveReqVO();
                attribute.setCode(key).setValue(value);
                standingbookAttributeByTypeId.forEach(standingbookAttributeDO -> {
                    if (standingbookAttributeDO.getCode().equals(key)){
                        attribute
                        .setSort(standingbookAttributeDO.getSort())
                        .setName(standingbookAttributeDO.getName())
                        .setAutoGenerated(standingbookAttributeDO.getAutoGenerated())
                        .setFormat(standingbookAttributeDO.getFormat())
                        .setOptions(standingbookAttributeDO.getOptions())
                        .setDescription(standingbookAttributeDO.getDescription())
                        .setNode(standingbookAttributeDO.getNode());
                    }
                });
                children.add(attribute);
            }
        }
        // 返回
        if (children.size() > 0) {
            children.forEach(child -> {
                child.setStandingbookId(standingbook.getId()).setIsRequired("0").setTypeId(standingbook.getTypeId());//默认不必填;
                standingbookAttributeService.create(child);
            });
        }
        return standingbook.getId();
    }

    @Override
    @Transactional
    public void updateStandingbook(Map <String,String>  updateReqVO) {
        // 校验存在
        validateStandingbookExists(Long.valueOf(updateReqVO.get("id")));
        // 更新
        StandingbookDO standingbook = new StandingbookDO();
        standingbook.setTypeId(Long.valueOf(updateReqVO.get("typeId")));
        standingbook.setId(Long.valueOf(updateReqVO.get("id")));
        standingbook.setLabelInfo(updateReqVO.get("labelInfo"));
        standingbook.setStage(Integer.valueOf(updateReqVO.get("stage")));
        standingbookMapper.updateById(standingbook);
        // 使用 entrySet() 遍历键和值
        List<StandingbookAttributeSaveReqVO> children= new ArrayList<>();
        for (Map.Entry<String, String> entry : updateReqVO.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (!entry.getKey() .equals("typeId")&&!entry.getKey() .equals("id")&&!entry.getKey().equals("labelInfo")){
                StandingbookAttributeSaveReqVO attribute = new StandingbookAttributeSaveReqVO();
                attribute.setCode(key).setValue(value);
                attribute.setStandingbookId(standingbook.getId());
                children.add(attribute);
            }
        }
        // 更新属性
        if (children.size() > 0) {
            children.forEach(child -> standingbookAttributeService.update(child));
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
    public List<StandingbookDO> getStandingbookList( Map<String,String> pageReqVO) {
        String typeId = pageReqVO.get("typeId");
        List<StandingbookAttributePageReqVO> children= new ArrayList<>();
        // 使用 entrySet() 遍历键和值
        for (Map.Entry<String, String> entry : pageReqVO.entrySet()) {
            System.out.println("Key: " + entry.getKey() + ", Value: " + entry.getValue());
            String key = entry.getKey();
            String value = entry.getValue();
            if (!entry.getKey() .equals("typeId")){
                StandingbookAttributePageReqVO attribute = new StandingbookAttributePageReqVO();
                attribute.setCode(key).setValue(value);
                children.add(attribute);
            }
        }
        List<StandingbookDO> standingbookDOS = standingbookAttributeService.getStandingbook(children, Long.valueOf(typeId));
        List<StandingbookDO> result = new ArrayList<>();
        for (StandingbookDO standingbookDO : standingbookDOS) {
            result.add(getStandingbook(standingbookDO.getId()));
        }
        return result;
    }

    @Override
    public List<StandingbookDO> getStandingbookListBy( Map<String,String> pageReqVO) {
        String typeId = null;
        List<StandingbookAttributePageReqVO> children= new ArrayList<>();
        // 使用 entrySet() 遍历键和值
        for (Map.Entry<String, String> entry : pageReqVO.entrySet()) {
            System.out.println("Key: " + entry.getKey() + ", Value: " + entry.getValue());
            String key = entry.getKey();
            String value = entry.getValue();
            if (!entry.getKey() .equals("typeId")){
                StandingbookAttributePageReqVO attribute = new StandingbookAttributePageReqVO();
                attribute.setCode(key).setValue(value);
                children.add(attribute);
            }
        }
        List<StandingbookDO> standingbookDOS = standingbookAttributeService.getStandingbook(children, null);
        List<StandingbookDO> result = new ArrayList<>();
        for (StandingbookDO standingbookDO : standingbookDOS) {
            result.add(getStandingbook(standingbookDO.getId()));
        }
        return result;
    }

    @Override
    public List<StandingbookWithAssociations> getStandingbookListWithAssociations(Map<String, String> pageReqVO) {
        // 获取台账列表
        List<StandingbookDO> standingbookDOS = getStandingbookListBy(pageReqVO);
        List<StandingbookWithAssociations> result = new ArrayList<>();

        for (StandingbookDO standingbookDO : standingbookDOS) {
            // 获取关联设备信息
            DeviceAssociationConfigurationDO association = deviceAssociationConfigurationService.getDeviceAssociationConfigurationByMeasurementInstrumentId(standingbookDO.getId());
            Long StandingbookId = standingbookDO.getId();
            String StandingbookName = standingbookTypeMapper.selectAttributeValueByCode(StandingbookId, "measuringInstrumentName");
            String measuringInstrumentId = standingbookTypeMapper.selectAttributeValueByCode(StandingbookId, "measuringInstrumentId");
            String tableType = standingbookTypeMapper.selectAttributeValueByCode(StandingbookId, "tableType");
            String valueType = standingbookTypeMapper.selectAttributeValueByCode(StandingbookId, "valueType");
            Integer stage = standingbookDO.getStage();
            String labelInfo = standingbookDO.getLabelInfo();

            StandingbookWithAssociations standingbookWithAssociations = new StandingbookWithAssociations();
            standingbookWithAssociations.setStandingbookId(StandingbookId);
            standingbookWithAssociations.setStandingbookName(StandingbookName);
            standingbookWithAssociations.setStandingbookTypeId(standingbookDO.getTypeId());
            StandingbookTypeDO standingbookType = standingbookTypeMapper.selectById(standingbookDO.getTypeId());
            standingbookWithAssociations.setStandingbookTypeName(standingbookType.getName());
            standingbookWithAssociations.setMeasuringInstrumentId(measuringInstrumentId);
            standingbookWithAssociations.setTableType(tableType);
            standingbookWithAssociations.setValueType(valueType);
            standingbookWithAssociations.setStage(stage);
            standingbookWithAssociations.setLabelInfo(labelInfo);


            // 解析 measurement 和 device 字段
            if (association != null) {
                if (association.getMeasurementIds() != null) {
                    try {
                        JsonNode measurementNode = objectMapper.readTree(association.getMeasurementIds());

                        List<AssociationData> children = new ArrayList<>();

                        for (JsonNode node : measurementNode) {
                            String measurementId = node.get("id").asText();
                            StandingbookDO measurementAttribute = getStandingbook(Long.valueOf(measurementId));
                            if (measurementAttribute != null) {
                                // todo 填充AssociationData
                                AssociationData associationData = new AssociationData();
                                associationData.setStandingbookId(measurementAttribute.getId());
                                associationData.setStandingbookName(measurementAttribute.getName());
                                String codeValue = standingbookTypeMapper.selectAttributeValueByCode(measurementAttribute.getId(), "measuringInstrumentId");
                                String stageValue = standingbookTypeMapper.selectAttributeValueByCode(measurementAttribute.getId(), "stage");
                                associationData.setStandingbookCode(codeValue);
                                associationData.setStage(stageValue);

                                children.add(associationData);
                            }
                        }
                        standingbookWithAssociations.setChildren(children);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if(association.getDeviceId() != null){
                    Long deviceId = association.getDeviceId();
                    String codeValue = standingbookTypeMapper.selectAttributeValueByCode(deviceId, "measuringInstrumentId");
                    StandingbookDO deviceAttribute = getStandingbook(deviceId);
                    standingbookWithAssociations.setDeviceId(deviceId);
                    standingbookWithAssociations.setDeviceName(deviceAttribute.getName());
                    standingbookWithAssociations.setDeviceCode(codeValue);
                }

                result.add(standingbookWithAssociations);
            }
        }
        return result;
    }

    @Override
    public Object importStandingbook(MultipartFile file, StandingbookRespVO pageReqVO) {
        StandingbookService proxy = context.getBean(StandingbookService.class);
        // TODO 导入功能实现
        Long typeId = pageReqVO.getTypeId();
        //模板
        List<StandingbookAttributeDO> attributes = standingbookAttributeService.getStandingbookAttributeByTypeId(typeId);

        // 获取标签信息
        String labelInfo = pageReqVO.getLabelInfo();
        JSONObject labelJson = JSONUtil.parseObj(labelInfo); // 使用JSONUtil解析JSON字符串

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
                // 处理标签信息
                for (Map.Entry<String, Object> entry : labelJson.entrySet()) {
                    StandingbookAttributeSaveReqVO attribute = new StandingbookAttributeSaveReqVO();
                    attribute.setName(entry.getKey());
                    attribute.setValue(entry.getValue().toString());
                    children.add(attribute);
                }
                for (int j = 0; j < attributes.size(); j++) {
                    Cell cell = row.getCell(j);
                    if (cell == null) continue;
                    StandingbookAttributeSaveReqVO attribute = new StandingbookAttributeSaveReqVO();
                    BeanUtils.copyProperties(attributes.get(j), attribute);
                    attribute.setStandingbookId(null);
                    if (!attribute.getFormat().equals(ApiConstants.FILE)) {
                        String cellValue = getCellValue(cell);
                        attribute.setValue(cellValue);
                    } else {
                        // TODO 读取图片文件
                    }
                    children.add(attribute);
                }
                proxy.create(saveReq);

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return "success";
    }

    @Override
    public void exportStandingbookExcel( Map <String,String> pageReqVO, HttpServletResponse response) {
        List<StandingbookDO> list =getStandingbookList(pageReqVO);
        Long typeId = Long.valueOf(pageReqVO.get("typeId"));
        List<StandingbookAttributeDO> attributes = standingbookAttributeService.getStandingbookAttributeByTypeId(typeId);

        // 获取标签信息
        String labelInfo = pageReqVO.get("labelInfo");
        JSONObject labelJson = JSONUtil.parseObj(labelInfo); // 使用JSONUtil解析JSON字符串

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
            int attributeCount = attributes.size();
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
            // 添加标签信息的表头
            int labelIndex = attributeCount;
            for (String labelKey : labelJson.keySet()) {
                Cell labelHeaderCell = headerRow.createCell(labelIndex++);
                labelHeaderCell.setCellValue(labelKey);
            }

            // 创建数据行
            for (int i = 0; i < list.size(); i++) {
                Row dataRow = sheet.createRow(i + 2);
                dataRow.setHeightInPoints(50); // 设置行高
                List<StandingbookAttributeDO> children = list.get(i).getChildren();
                for (int j = 0; j < children.size(); j++) {
                    StandingbookAttributeDO standingbookAttributeDO = children.get(j);
                    Cell cell = dataRow.createCell(j); // 从第1列开始
                    if (ApiConstants.FILE.equals(standingbookAttributeDO.getFormat())) {
//                        CommonResult result = fileApi.getFile(standingbookAttributeDO.getFileId());
//                        if (result == null || result.getData() == null)continue;
//                        JSONObject file = (JSONObject) JSONUtil.parse(result.getData());
//                        CommonResult<byte[]> fileId = fileApi.getFileContent(Long.valueOf(file.getStr("configId")),file.getStr("path"));
//                        if (fileId == null || fileId.getData() == null)continue;
//                        byte[] bytes = fileId.getData();
//                        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
//                        // 缩放图片
//                        ScaledImageDataVO scaledImageData = scaleImage(image);
//
//                        //将缩放后的图片转换为bytes
//                        byte[] imageBytes = processImageToBytes(scaledImageData.getScaledImage());
//
//                        int pictureIdx = workbook.addPicture(imageBytes, Workbook.PICTURE_TYPE_PNG);
//                        CreationHelper helper = workbook.getCreationHelper();
//                        // 创建绘图对象
//                        Drawing<?> drawing = sheet.createDrawingPatriarch();
//
//                        // 定位图片 (行1, 列1, 宽度和高度可根据需要调整)
//
//                        ClientAnchor anchor = helper.createClientAnchor();
//                        anchor.setCol1(j); // 列号从0开始，1表示第一列
//                        anchor.setRow1(i + 2); // 行号从0开始，1表示第一行
//                        anchor.setCol2(j); // 列号从0开始，1表示第一列
//                        anchor.setRow2(i + 2); // 行号从0开始，1表示第一行
//                        // 插入图片
//                        Picture pict = drawing.createPicture(anchor, pictureIdx);
//                        pict.resize(); // 调整图片大小以适应单元格
                    } else {
                        cell.setCellValue(standingbookAttributeDO.getValue());
                    }
                }
                // 添加标签信息的数据
                int labelDataIndex = attributeCount;
                for (Map.Entry<String, Object> entry : labelJson.entrySet()) {
                    Cell labelCell = dataRow.createCell(labelDataIndex++);
                    labelCell.setCellValue(entry.getValue().toString());
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

        List<Tree<Long>> labelTree = labelService.getLabelTree(false, null, null);
        // 提取根标签的 code 作为表头
        List<String> rootLabelNames = new ArrayList<>();
        for (Tree<Long> tree : labelTree) {
            if (tree.getParentId() == 0) { // 只处理根标签
                rootLabelNames.add(String.valueOf(tree.getName())); // 提取 code 字段
            }
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
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 2, 10));
            Cell hintCell = firstRow.createCell(2);
            hintCell.setCellValue("表头黄色的为必填项，请勿修改模板编号，否则无法导入数据。请从第三行开始填写数据，多选时请使用&符号分隔。（暂不支持导入图片和文件）");
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
                if (ApiConstants.MULTIPLE.equals(column.getFormat())) {
                    cell.setCellValue(column.getName()+"("+column.getOptions().replaceAll(";","&")+")");
                }else {
                    cell.setCellValue(column.getName());
                }
                // 设置单元格样式
                CellStyle style = workbook.createCellStyle();
                if (ApiConstants.YES.equals(column.getIsRequired())&& !column.getFormat().equals(ApiConstants.FILE) && !column.getFormat().equals(ApiConstants.PICTURE)) {
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
                // 注意: Apache POI 目前不支持直接设置为多选下拉框。对于多选，用户需要通过VBA或者手动配置。
                cell.setCellStyle(style);
            }
            // 添加根标签作为表头
            int labelStartColumn = attributes.size(); // 标签从台账属性之后开始
            for (int i = 0; i < rootLabelNames.size(); i++) {
                Cell labelHeaderCell = headerRow.createCell(labelStartColumn + i);
                labelHeaderCell.setCellValue(rootLabelNames.get(i));
            }

            // 输出
            response.setContentType("application/octet-stream; charset=utf-8");
            response.setHeader("Content-Disposition", "attachment; filename="+ URLEncoder.encode("模板编号-" + typeId + "-台账导入模板.xlsx","UTF-8"));

            OutputStream os = response.getOutputStream();
            workbook.write(os);
            os.flush();
            os.close();
//            try (FileOutputStream fileOut = new FileOutputStream("D:\\破烂\\项目\\"+typeId+"-template.xlsx")) {
//                workbook.write(fileOut);
//            }
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
