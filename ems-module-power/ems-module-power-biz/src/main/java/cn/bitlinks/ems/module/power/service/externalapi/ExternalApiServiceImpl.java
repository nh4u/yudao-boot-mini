package cn.bitlinks.ems.module.power.service.externalapi;

import cn.bitlinks.ems.framework.common.pojo.PageResult;
import cn.bitlinks.ems.framework.common.util.http.HttpUtils;
import cn.bitlinks.ems.framework.common.util.object.BeanUtils;
import cn.bitlinks.ems.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.bitlinks.ems.module.power.controller.admin.externalapi.vo.ExternalApiPageReqVO;
import cn.bitlinks.ems.module.power.controller.admin.externalapi.vo.ExternalApiSaveReqVO;
import cn.bitlinks.ems.module.power.controller.admin.externalapi.vo.ProductYieldMeta;
import cn.bitlinks.ems.module.power.dal.dataobject.externalapi.ExternalApiDO;
import cn.bitlinks.ems.module.power.dal.mysql.externalapi.ExternalApiMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.bitlinks.ems.module.power.enums.CommonConstants.GET;
import static cn.bitlinks.ems.module.power.enums.CommonConstants.POST;
import static cn.bitlinks.ems.module.power.enums.ErrorCodeConstants.*;


/**
 * @author liumingqiang
 */
@Service
@Validated
public class ExternalApiServiceImpl implements ExternalApiService {

    @Resource
    private ExternalApiMapper externalApiMapper;

    @Override
    public ExternalApiDO createExternalApi(ExternalApiSaveReqVO createReqVO) {


        // 接口编码不能重复 校验
        Long count = externalApiMapper.selectCount(new LambdaQueryWrapper<ExternalApiDO>()
                .eq(ExternalApiDO::getCode, createReqVO.getCode()));

        if (count > 0) {
            throw exception(EXTERNAL_API_CODE_REPEAT);
        }

        // 转换请求对象到数据对象
        ExternalApiDO externalApi = BeanUtils.toBean(createReqVO, ExternalApiDO.class);
        // 插入数据库
        externalApiMapper.insert(externalApi);

        // 返回记录 ID
        return externalApi;
    }

    @Override
    public void updateExternalApi(ExternalApiSaveReqVO updateReqVO) {
        Long id = updateReqVO.getId();
        // Step 1: 校验凭证是否存在
        validateExternalApiExists(id);

        // 接口编码不能重复 校验
        Long count = externalApiMapper.selectCount(new LambdaQueryWrapper<ExternalApiDO>()
                .eq(ExternalApiDO::getCode, updateReqVO.getCode())
                .ne(ExternalApiDO::getId, id));

        if (count > 0) {
            throw exception(EXTERNAL_API_CODE_REPEAT);
        }

        // Step 5: 更新凭证记录
        ExternalApiDO updateObj = BeanUtils.toBean(updateReqVO, ExternalApiDO.class);
        externalApiMapper.updateById(updateObj);
    }

    @Override
    public void deleteExternalApi(Long id) {
        // 删除
        externalApiMapper.deleteById(id);
    }

    @Override
    public ExternalApiDO getExternalApi(Long id) {
        return externalApiMapper.selectById(id);
    }

    @Override
    public PageResult<ExternalApiDO> getExternalApiPage(ExternalApiPageReqVO pageReqVO) {
        return externalApiMapper.selectPage(pageReqVO);
    }

    @Override
    public Object testExternalApi(ExternalApiSaveReqVO createReqVO) {

        String method = createReqVO.getMethod();
        String url = createReqVO.getUrl();

        Map<String, String> header = new HashMap<>();
        header.put("Content-Type", "application/json;charset=UTF-8");

        if (POST.equalsIgnoreCase(method)) {
            // 1.当是 post时
            String body = createReqVO.getBody();
            return postResponse(url, header, body);

        } else if (GET.equalsIgnoreCase(method)) {
            // 2.当是 get时
            return getResponse(url, header);

        } else {
            // 不属于上面两种方法
            throw exception(EXTERNAL_API_HTTP_METHOD_ERROR);
        }
    }

    @Override
    public String getProductYieldUrl() {
        ExternalApiDO chanliang = externalApiMapper.selectOne(new LambdaQueryWrapperX<ExternalApiDO>()
                .eq(ExternalApiDO::getCode, "chanliang")
                .orderByDesc(ExternalApiDO::getCreateTime)
                .last("limit 1"));

        return chanliang.getUrl();
    }


    /**
     * 测试接口
     *
     * @return
     */
    @Override
    public Object getAllOut() {
        JSONObject top = new JSONObject();

        String body = "{\n" +
                "    \"8吋\":[\n" +
                "        {\n" +
                "            \"FABOUTTIME\":\"202101\",\n" +
                "            \"PLAN_QTY\":40000,\n" +
                "            \"LOT_QTY\":16896\n" +
                "        },\n" +
                "        {\n" +
                "            \"FABOUTTIME\":\"202102\",\n" +
                "            \"PLAN_QTY\":30200,\n" +
                "            \"LOT_QTY\":16785\n" +
                "        },\n" +
                "        {\n" +
                "            \"FABOUTTIME\":\"202103\",\n" +
                "            \"PLAN_QTY\":50770,\n" +
                "            \"LOT_QTY\":16918\n" +
                "        },\n" +
                "        {\n" +
                "            \"FABOUTTIME\":\"202104\",\n" +
                "            \"PLAN_QTY\":50000,\n" +
                "            \"LOT_QTY\":23629\n" +
                "        },\n" +
                "        {\n" +
                "            \"FABOUTTIME\":\"202105\",\n" +
                "            \"PLAN_QTY\":50038,\n" +
                "            \"LOT_QTY\":23214\n" +
                "        },\n" +
                "        {\n" +
                "            \"FABOUTTIME\":\"202106\",\n" +
                "            \"PLAN_QTY\":60042,\n" +
                "            \"LOT_QTY\":25952\n" +
                "        },\n" +
                "        {\n" +
                "            \"FABOUTTIME\":\"202107\",\n" +
                "            \"PLAN_QTY\":60138,\n" +
                "            \"LOT_QTY\":30826\n" +
                "        },\n" +
                "        {\n" +
                "            \"FABOUTTIME\":\"202108\",\n" +
                "            \"PLAN_QTY\":63712,\n" +
                "            \"LOT_QTY\":31060\n" +
                "        },\n" +
                "        {\n" +
                "            \"FABOUTTIME\":\"202109\",\n" +
                "            \"PLAN_QTY\":80000,\n" +
                "            \"LOT_QTY\":31138\n" +
                "        },\n" +
                "        {\n" +
                "            \"FABOUTTIME\":\"202110\",\n" +
                "            \"PLAN_QTY\":19000,\n" +
                "            \"LOT_QTY\":39089\n" +
                "        },\n" +
                "        {\n" +
                "            \"FABOUTTIME\":\"202111\",\n" +
                "            \"PLAN_QTY\":48000,\n" +
                "            \"LOT_QTY\":38718\n" +
                "        },\n" +
                "        {\n" +
                "            \"FABOUTTIME\":\"202112\",\n" +
                "            \"PLAN_QTY\":100000,\n" +
                "            \"LOT_QTY\":46655\n" +
                "        },\n" +
                "        {\n" +
                "            \"FABOUTTIME\":\"202201\",\n" +
                "            \"PLAN_QTY\":80000,\n" +
                "            \"LOT_QTY\":39340\n" +
                "        },\n" +
                "        {\n" +
                "            \"FABOUTTIME\":\"202202\",\n" +
                "            \"PLAN_QTY\":60200,\n" +
                "            \"LOT_QTY\":30016\n" +
                "        },\n" +
                "        {\n" +
                "            \"FABOUTTIME\":\"202203\",\n" +
                "            \"PLAN_QTY\":80052,\n" +
                "            \"LOT_QTY\":31944\n" +
                "        },\n" +
                "        {\n" +
                "            \"FABOUTTIME\":\"202204\",\n" +
                "            \"PLAN_QTY\":80036,\n" +
                "            \"LOT_QTY\":41299\n" +
                "        },\n" +
                "        {\n" +
                "            \"FABOUTTIME\":\"202205\",\n" +
                "            \"PLAN_QTY\":86268,\n" +
                "            \"LOT_QTY\":45469\n" +
                "        },\n" +
                "        {\n" +
                "            \"FABOUTTIME\":\"202206\",\n" +
                "            \"PLAN_QTY\":88400,\n" +
                "            \"LOT_QTY\":44911\n" +
                "        },\n" +
                "        {\n" +
                "            \"FABOUTTIME\":\"202207\",\n" +
                "            \"PLAN_QTY\":92000,\n" +
                "            \"LOT_QTY\":45827\n" +
                "        },\n" +
                "        {\n" +
                "            \"FABOUTTIME\":\"202208\",\n" +
                "            \"PLAN_QTY\":62000,\n" +
                "            \"LOT_QTY\":37798\n" +
                "        },\n" +
                "        {\n" +
                "            \"FABOUTTIME\":\"202209\",\n" +
                "            \"PLAN_QTY\":10026,\n" +
                "            \"LOT_QTY\":7301\n" +
                "        },\n" +
                "        {\n" +
                "            \"FABOUTTIME\":\"202210\",\n" +
                "            \"PLAN_QTY\":40050,\n" +
                "            \"LOT_QTY\":23488\n" +
                "        },\n" +
                "        {\n" +
                "            \"FABOUTTIME\":\"202211\",\n" +
                "            \"PLAN_QTY\":40000,\n" +
                "            \"LOT_QTY\":20110\n" +
                "        },\n" +
                "        {\n" +
                "            \"FABOUTTIME\":\"202212\",\n" +
                "            \"PLAN_QTY\":92014,\n" +
                "            \"LOT_QTY\":47820\n" +
                "        },\n" +
                "        {\n" +
                "            \"FABOUTTIME\":\"202301\",\n" +
                "            \"PLAN_QTY\":84700,\n" +
                "            \"LOT_QTY\":37259.75\n" +
                "        },\n" +
                "        {\n" +
                "            \"FABOUTTIME\":\"202302\",\n" +
                "            \"PLAN_QTY\":71678,\n" +
                "            \"LOT_QTY\":33616.35\n" +
                "        },\n" +
                "        {\n" +
                "            \"FABOUTTIME\":\"202303\",\n" +
                "            \"PLAN_QTY\":101500,\n" +
                "            \"LOT_QTY\":35216.25\n" +
                "        },\n" +
                "        {\n" +
                "            \"FABOUTTIME\":\"202304\",\n" +
                "            \"PLAN_QTY\":62645,\n" +
                "            \"LOT_QTY\":33183.65\n" +
                "        },\n" +
                "        {\n" +
                "            \"FABOUTTIME\":\"202305\",\n" +
                "            \"PLAN_QTY\":80000,\n" +
                "            \"LOT_QTY\":34755.39\n" +
                "        },\n" +
                "        {\n" +
                "            \"FABOUTTIME\":\"202306\",\n" +
                "            \"PLAN_QTY\":80000,\n" +
                "            \"LOT_QTY\":37134\n" +
                "        },\n" +
                "        {\n" +
                "            \"FABOUTTIME\":\"202307\",\n" +
                "            \"PLAN_QTY\":80000,\n" +
                "            \"LOT_QTY\":32292\n" +
                "        },\n" +
                "        {\n" +
                "            \"FABOUTTIME\":\"202308\",\n" +
                "            \"PLAN_QTY\":89500,\n" +
                "            \"LOT_QTY\":39596\n" +
                "        },\n" +
                "        {\n" +
                "            \"FABOUTTIME\":\"202309\",\n" +
                "            \"PLAN_QTY\":70366,\n" +
                "            \"LOT_QTY\":37737.6\n" +
                "        },\n" +
                "        {\n" +
                "            \"FABOUTTIME\":\"202310\",\n" +
                "            \"PLAN_QTY\":64100,\n" +
                "            \"LOT_QTY\":27830\n" +
                "        },\n" +
                "        {\n" +
                "            \"FABOUTTIME\":\"202311\",\n" +
                "            \"PLAN_QTY\":63910,\n" +
                "            \"LOT_QTY\":28309.03\n" +
                "        },\n" +
                "        {\n" +
                "            \"FABOUTTIME\":\"202312\",\n" +
                "            \"PLAN_QTY\":90116,\n" +
                "            \"LOT_QTY\":35558.9\n" +
                "        },\n" +
                "        {\n" +
                "            \"FABOUTTIME\":\"202401\",\n" +
                "            \"PLAN_QTY\":60746,\n" +
                "            \"LOT_QTY\":2887\n" +
                "        }\n" +
                "    ],\n" +
                "    \"12吋\":[\n" +
                "\n" +
                "    ]\n" +
                "}";
        JSONObject jsonObject = JSON.parseObject(body);

        List<ProductYieldMeta> eightList = (List<ProductYieldMeta>) JSONPath.eval(jsonObject, "$.8吋");
        List<ProductYieldMeta> twelveList = (List<ProductYieldMeta>) JSONPath.eval(jsonObject, "$.12吋");
        top.put("8吋", eightList);
        top.put("12吋", twelveList);

        return top;
    }

    private JSONObject postResponse(String url, Map<String, String> header, String body) {
        try {
            String response = HttpUtils.post(url, header, body);
            return JSON.parseObject(response);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private JSONObject getResponse(String url, Map<String, String> header) {
        try {
            String response = HttpUtils.get(url, header);
            return JSON.parseObject(response);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    private void validateExternalApiExists(Long id) {
        if (externalApiMapper.selectById(id) == null) {
            throw exception(EXTERNAL_API_NOT_EXISTS);
        }
    }
}