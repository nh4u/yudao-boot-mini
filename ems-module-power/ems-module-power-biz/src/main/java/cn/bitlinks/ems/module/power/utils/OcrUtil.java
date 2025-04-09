package cn.bitlinks.ems.module.power.utils;

import cn.bitlinks.ems.framework.common.exception.ErrorCode;
import cn.bitlinks.ems.module.power.config.OcrProperties;
import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import static cn.bitlinks.ems.framework.common.exception.util.ServiceExceptionUtil.exception;

/**
 * @Title: ydme-ems
 * @description:
 * @Author: Mingqiang LIU
 * @Date 2025/04/09 18:27
 **/

public class OcrUtil {

    public static JSONObject ocrRecognition(String url, OcrProperties ocrProperties) {
        try {
            // 下载网络文件到临时文件
            byte[] fileBytes = HttpUtil.downloadBytes(url);

            // 构建POST请求
            HttpRequest request = HttpRequest.post(ocrProperties.getUploadUrl())
                    .header("Authorization", "Bearer " + ocrProperties.getToken())
                    .form("file", fileBytes, FileUtil.getName(url));

            // 发送请求并获取响应
            HttpResponse response = request.execute();

            // 返回响应内容

            return JSONUtil.parseObj(response.body());

        } catch (Exception e) {
            e.printStackTrace();
            ErrorCode message = new ErrorCode(1_001_601_005, "文件上传失败: " + e.getMessage());
            throw exception(message);
        }


    }
}
